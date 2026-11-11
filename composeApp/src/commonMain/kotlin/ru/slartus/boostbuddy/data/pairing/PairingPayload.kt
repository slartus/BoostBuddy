package ru.slartus.boostbuddy.data.pairing

import kotlinx.serialization.Serializable

@Serializable
internal data class PairingPayload(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long? = null,
    val deviceId: String,
    val source: String,
    val iat: Long,
) {
    override fun toString(): String =
        "PairingPayload(source=$source, iat=$iat, expiresAt=$expiresAt)"

    companion object {
        const val SOURCE_WEB = "web"
        const val SOURCE_APP = "app"
    }
}

private const val MILLIS_EPOCH_THRESHOLD = 100_000_000_000L

internal fun normalizeEpochSeconds(value: Long?): Long? {
    if (value == null || value <= 0) return null
    return if (value > MILLIS_EPOCH_THRESHOLD) value / 1000 else value
}
