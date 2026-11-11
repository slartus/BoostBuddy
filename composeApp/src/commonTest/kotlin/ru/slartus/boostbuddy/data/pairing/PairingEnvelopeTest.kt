package ru.slartus.boostbuddy.data.pairing

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class PairingEnvelopeTest {

    private val key = ByteArray(32) { it.toByte() }
    private val sid = PairingBase64.encode(ByteArray(16) { (0xa0 + it).toByte() })
    private val nonce = ByteArray(12) { (0x10 + it).toByte() }
    private val plain =
        """{"accessToken":"a","refreshToken":"r","expiresAt":null,"deviceId":"d","source":"web","iat":1700000000}"""

    @Test
    fun seal_matchesWebCryptoVector() {
        assertEquals(WEB_CRYPTO_SEALED, PairingEnvelope.seal(key, sid, plain.encodeToByteArray(), nonce))
    }

    @Test
    fun open_acceptsWebCryptoVector() {
        val opened = PairingEnvelope.open(key, sid, WEB_CRYPTO_SEALED).decodeToString()

        assertEquals(plain, opened)
        val payload = Json.decodeFromString<PairingPayload>(opened)
        assertEquals(PairingPayload.SOURCE_WEB, payload.source)
        assertNull(payload.expiresAt)
    }

    @Test
    fun sealThenOpen_roundtrips() {
        val randomKey = PairingEnvelope.newKey()
        val randomSid = PairingEnvelope.newSid()

        val sealed = PairingEnvelope.seal(randomKey, randomSid, plain.encodeToByteArray())

        assertContentEquals(plain.encodeToByteArray(), PairingEnvelope.open(randomKey, randomSid, sealed))
    }

    @Test
    fun seal_usesFreshNonceEachTime() {
        val first = PairingEnvelope.seal(key, sid, plain.encodeToByteArray())
        val second = PairingEnvelope.seal(key, sid, plain.encodeToByteArray())

        assertNotEquals(first, second)
    }

    @Test
    fun newKeyAndSid_haveExpectedSizes() {
        assertEquals(32, PairingEnvelope.newKey().size)
        assertEquals(16, PairingBase64.decodeOrNull(PairingEnvelope.newSid())?.size)
        assertEquals(22, PairingEnvelope.newSid().length)
    }

    @Test
    fun open_rejectsWrongSid() {
        val otherSid = PairingBase64.encode(ByteArray(16) { 1 })

        assertFailsWith<PairingEnvelopeException> { PairingEnvelope.open(key, otherSid, WEB_CRYPTO_SEALED) }
    }

    @Test
    fun open_rejectsWrongKey() {
        val otherKey = key.copyOf().also { it[0] = 99 }

        assertFailsWith<PairingEnvelopeException> { PairingEnvelope.open(otherKey, sid, WEB_CRYPTO_SEALED) }
    }

    @Test
    fun open_rejectsTamperedTag() {
        val bytes = PairingBase64.decodeOrNull(WEB_CRYPTO_SEALED)!!
        bytes[bytes.lastIndex] = (bytes[bytes.lastIndex].toInt() xor 1).toByte()

        assertFailsWith<PairingEnvelopeException> {
            PairingEnvelope.open(key, sid, PairingBase64.encode(bytes))
        }
    }

    @Test
    fun open_rejectsTamperedCiphertext() {
        val bytes = PairingBase64.decodeOrNull(WEB_CRYPTO_SEALED)!!
        bytes[20] = (bytes[20].toInt() xor 1).toByte()

        assertFailsWith<PairingEnvelopeException> {
            PairingEnvelope.open(key, sid, PairingBase64.encode(bytes))
        }
    }

    @Test
    fun open_rejectsWrongVersion() {
        val bytes = PairingBase64.decodeOrNull(WEB_CRYPTO_SEALED)!!
        bytes[0] = 2

        assertFailsWith<PairingEnvelopeException> {
            PairingEnvelope.open(key, sid, PairingBase64.encode(bytes))
        }
    }

    @Test
    fun open_rejectsTruncated() {
        val bytes = PairingBase64.decodeOrNull(WEB_CRYPTO_SEALED)!!

        assertFailsWith<PairingEnvelopeException> {
            PairingEnvelope.open(key, sid, PairingBase64.encode(bytes.copyOf(bytes.size - 1)))
        }
        assertFailsWith<PairingEnvelopeException> {
            PairingEnvelope.open(key, sid, PairingBase64.encode(bytes.copyOf(1 + 12 + 15)))
        }
    }

    @Test
    fun open_rejectsMalformedBase64() {
        assertFailsWith<PairingEnvelopeException> { PairingEnvelope.open(key, sid, "$WEB_CRYPTO_SEALED==") }
        assertFailsWith<PairingEnvelopeException> { PairingEnvelope.open(key, sid, "not base64 !") }
        assertFailsWith<PairingEnvelopeException> { PairingEnvelope.open(key, sid, "") }
    }

    @Test
    fun open_rejectsWrongKeySize() {
        val error = assertFailsWith<PairingEnvelopeException> {
            PairingEnvelope.open(key.copyOf(16), sid, WEB_CRYPTO_SEALED)
        }

        assertEquals("Key must be 32 bytes", error.message)
    }

    private companion object {
        const val WEB_CRYPTO_SEALED =
            "ARAREhMUFRYXGBkaGwbc-XUqrEnAnhpjeGFbU3G2cmIsaacxw4KKijMxPzG1ctN1cdeKTIoGRvsj7-tSbzGGKQaP53XQ2HtMWJDy7Q8nwh1Ha5IpRwL1R52949KcStzwj0q_O1v00hou2bTTJUw3foaC1-ecgZ2Q9bHLFbvyFL4nnF4"
    }
}
