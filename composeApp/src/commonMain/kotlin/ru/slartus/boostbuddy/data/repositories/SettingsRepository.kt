package ru.slartus.boostbuddy.data.repositories

import com.russhwolf.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class SettingsRepository(
    private val settings: Settings
) {
    suspend fun getString(key: String): String? = withContext(Dispatchers.IO) {
        settings.getStringOrNull(key)
    }

    suspend fun putString(key: String, value: String) = withContext(Dispatchers.IO) {
        settings.putString(key, value)
    }
}

suspend fun SettingsRepository.getAccessToken(): String? = getString("access_token")

suspend fun SettingsRepository.putAccessToken(value: String) = putString("access_token", value)

suspend fun SettingsRepository.getWebCookie(): String? = getString("web_cookie")

suspend fun SettingsRepository.putWebCookie(value: String) = putString("web_cookie", value)