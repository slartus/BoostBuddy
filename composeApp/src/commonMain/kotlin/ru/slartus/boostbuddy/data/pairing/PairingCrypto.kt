package ru.slartus.boostbuddy.data.pairing

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal class PairingEnvelopeException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

@OptIn(ExperimentalEncodingApi::class)
internal object PairingBase64 {
    private val codec = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    fun encode(bytes: ByteArray): String = codec.encode(bytes)

    fun decodeOrNull(text: String): ByteArray? =
        try {
            codec.decode(text)
        } catch (e: IllegalArgumentException) {
            null
        }
}

@OptIn(DelicateCryptographyApi::class)
internal object PairingEnvelope {
    const val VERSION: Byte = 1
    const val KEY_SIZE = 32
    const val SID_SIZE = 16
    const val NONCE_SIZE = 12
    const val TAG_SIZE = 16

    private val AAD_PREFIX = "BB-PAIR".encodeToByteArray()

    fun newKey(): ByteArray = CryptographyRandom.nextBytes(KEY_SIZE)

    fun newSid(): String = PairingBase64.encode(CryptographyRandom.nextBytes(SID_SIZE))

    fun seal(key: ByteArray, sid: String, plain: ByteArray): String =
        seal(key, sid, plain, CryptographyRandom.nextBytes(NONCE_SIZE))

    internal fun seal(key: ByteArray, sid: String, plain: ByteArray, nonce: ByteArray): String {
        require(nonce.size == NONCE_SIZE) { "Nonce must be $NONCE_SIZE bytes" }
        val ciphertext = cipher(key).encryptWithIvBlocking(nonce, plain, associatedData(sid))
        return PairingBase64.encode(byteArrayOf(VERSION) + nonce + ciphertext)
    }

    fun open(key: ByteArray, sid: String, sealed: String): ByteArray {
        val bytes = PairingBase64.decodeOrNull(sealed)
            ?: throw PairingEnvelopeException("Malformed envelope")
        if (bytes.size < 1 + NONCE_SIZE + TAG_SIZE) throw PairingEnvelopeException("Envelope too short")
        if (bytes[0] != VERSION) throw PairingEnvelopeException("Unsupported envelope version")
        val nonce = bytes.copyOfRange(1, 1 + NONCE_SIZE)
        val ciphertext = bytes.copyOfRange(1 + NONCE_SIZE, bytes.size)
        return try {
            cipher(key).decryptWithIvBlocking(nonce, ciphertext, associatedData(sid))
        } catch (e: Exception) {
            throw PairingEnvelopeException("Envelope authentication failed", e)
        }
    }

    private fun cipher(key: ByteArray): AES.IvAuthenticatedCipher {
        if (key.size != KEY_SIZE) throw PairingEnvelopeException("Key must be $KEY_SIZE bytes")
        return CryptographyProvider.Default
            .get(AES.GCM)
            .keyDecoder()
            .decodeFromByteArrayBlocking(AES.Key.Format.RAW, key)
            .cipher()
    }

    private fun associatedData(sid: String): ByteArray =
        AAD_PREFIX + VERSION + sid.encodeToByteArray()
}
