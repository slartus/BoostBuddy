package ru.slartus.boostbuddy.data.repositories

internal data class AuthSession(
    val accessToken: String?,
    val refreshToken: String?,
    val expiresAt: Long?,
    val deviceId: String?,
) {
    override fun toString(): String =
        "AuthSession(hasAccessToken=${accessToken != null}, hasRefreshToken=${refreshToken != null}, expiresAt=$expiresAt)"
}
