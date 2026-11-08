package ru.slartus.boostbuddy.data.auth

import io.ktor.http.decodeURLQueryComponent
import kotlinx.serialization.json.Json
import ru.slartus.boostbuddy.data.log.logger
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.models.AuthResponse
import ru.slartus.boostbuddy.utils.WebManager

private const val BOOSTY_URL = "https://boosty.to/"
private const val AUTH_COOKIE = "auth"
private const val CLIENT_ID_COOKIE = "_clientId"

internal fun Map<String, String>.boostyClientId(): String? =
    get(CLIENT_ID_COOKIE)
        ?.decodeURLQueryComponent()
        ?.trim()
        ?.removeSurrounding("\"")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

internal suspend fun migrateWebSessionDeviceId(settingsRepository: SettingsRepository, json: Json) {
    if (settingsRepository.isWebDeviceIdMigrated()) return
    runCatching {
        val storedRefreshToken = settingsRepository.getRefreshToken()
        if (storedRefreshToken.isNullOrEmpty()) return@runCatching
        val cookies = WebManager.getCookies(BOOSTY_URL)?.let(::parseCookieHeader) ?: return@runCatching
        val webRefreshToken = cookies[AUTH_COOKIE]
            ?.let { json.decodeFromString<AuthResponse>(it.decodeURLQueryComponent()).refreshToken }
        if (webRefreshToken != storedRefreshToken) return@runCatching
        val clientId = cookies.boostyClientId() ?: return@runCatching
        if (clientId != settingsRepository.getOrCreateDeviceId()) {
            settingsRepository.putDeviceId(clientId)
            logger.i("Device id replaced with web client id for web session")
        }
    }.onFailure { logger.e(it, "migrateWebSessionDeviceId") }
    settingsRepository.setWebDeviceIdMigrated()
}

private fun parseCookieHeader(cookies: String): Map<String, String> =
    cookies.split(";")
        .mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            part.substring(0, separator).trim() to part.substring(separator + 1).trim()
        }
        .toMap()
