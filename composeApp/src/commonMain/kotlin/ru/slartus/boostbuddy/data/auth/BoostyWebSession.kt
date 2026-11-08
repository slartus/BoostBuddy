package ru.slartus.boostbuddy.data.auth

import io.ktor.http.decodeURLQueryComponent
import kotlinx.serialization.json.Json
import ru.slartus.boostbuddy.data.log.logger
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.models.AuthResponse
import ru.slartus.boostbuddy.utils.WebManager
import ru.slartus.boostbuddy.utils.WebViewUnavailableException
import kotlin.coroutines.cancellation.CancellationException

private const val BOOSTY_URL = "https://boosty.to/"
private const val AUTH_COOKIE = "auth"
private const val CLIENT_ID_COOKIE = "_clientId"

internal fun parseCookieHeader(cookies: String): Map<String, String> =
    cookies.split(";")
        .mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator < 0) return@mapNotNull null
            val name = part.substring(0, separator).trim()
            if (name.isEmpty()) return@mapNotNull null
            name to part.substring(separator + 1).trim()
        }
        .toMap()

internal fun Map<String, String>.boostyClientId(): String? =
    get(CLIENT_ID_COOKIE)
        ?.decodeURLQueryComponent()
        ?.trim()
        ?.removeSurrounding("\"")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

internal suspend fun migrateWebSessionDeviceId(settingsRepository: SettingsRepository, json: Json) {
    if (settingsRepository.isWebDeviceIdMigrated()) return
    try {
        replaceDeviceIdWithWebClientId(settingsRepository, json)
        settingsRepository.setWebDeviceIdMigrated()
    } catch (e: CancellationException) {
        throw e
    } catch (e: WebViewUnavailableException) {
        logger.w(e, "migrateWebSessionDeviceId: WebView unavailable")
        settingsRepository.setWebDeviceIdMigrated()
    } catch (e: Exception) {
        logger.e(e, "migrateWebSessionDeviceId")
    }
}

private suspend fun replaceDeviceIdWithWebClientId(settingsRepository: SettingsRepository, json: Json) {
    val storedRefreshToken = settingsRepository.getRefreshToken()
    if (storedRefreshToken.isNullOrEmpty()) return
    val cookies = WebManager.getCookies(BOOSTY_URL)?.let(::parseCookieHeader) ?: return
    val webRefreshToken = cookies[AUTH_COOKIE]?.let { authCookie ->
        runCatching {
            json.decodeFromString<AuthResponse>(authCookie.decodeURLQueryComponent()).refreshToken
        }.getOrNull()
    }
    if (webRefreshToken != storedRefreshToken) return
    val clientId = cookies.boostyClientId() ?: return
    if (clientId != settingsRepository.getOrCreateDeviceId()) {
        settingsRepository.putDeviceId(clientId)
        logger.i("Device id replaced with web client id for web session")
    }
}
