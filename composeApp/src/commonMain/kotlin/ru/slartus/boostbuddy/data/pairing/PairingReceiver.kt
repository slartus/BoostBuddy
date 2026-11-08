package ru.slartus.boostbuddy.data.pairing

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

internal interface PairingReceiver {
    val events: Flow<PairingReceiverEvent>

    suspend fun start(): Result<PairingSession>

    fun stop()
}

internal data class PairingSession(
    val pageUrl: String,
    val host: String,
    val port: Int,
    val sid: String,
    val expiresAtMs: Long,
) {
    override fun toString(): String =
        "PairingSession(host=$host, port=$port, sid=$sid, expiresAtMs=$expiresAtMs)"
}

internal sealed class PairingReceiverEvent {
    data class Received(val payload: PairingPayload) : PairingReceiverEvent()
    data object Expired : PairingReceiverEvent()
    data object TooManyAttempts : PairingReceiverEvent()
}

internal sealed class PairingReceiverException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    class NoLan : PairingReceiverException("No private LAN IPv4 address")
    class NoFreePort(cause: Throwable?) : PairingReceiverException("No free pairing port", cause)
}

internal object PairingPayloadValidator {
    private val json = Json { ignoreUnknownKeys = true }

    fun openOrNull(key: ByteArray, sid: String, sealed: String): PairingPayload? {
        val plain = try {
            PairingEnvelope.open(key, sid, sealed)
        } catch (e: PairingEnvelopeException) {
            return null
        }
        val payload = try {
            json.decodeFromString<PairingPayload>(plain.decodeToString())
        } catch (e: IllegalArgumentException) {
            return null
        } finally {
            plain.fill(0)
        }
        if (payload.accessToken.isBlank() || payload.refreshToken.isBlank() || payload.deviceId.isBlank()) return null
        if (payload.source != PairingPayload.SOURCE_WEB && payload.source != PairingPayload.SOURCE_APP) return null
        return payload.copy(expiresAt = normalizeEpochSeconds(payload.expiresAt))
    }
}
