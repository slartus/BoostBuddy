package ru.slartus.boostbuddy.data.repositories

import com.russhwolf.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

internal class SettingsRepository(
    private val settings: Settings
) {
    private val tokenBus: MutableStateFlow<String?> = MutableStateFlow(
        null
    )

    val tokenFlow: StateFlow<String?>
        get() = tokenBus.asStateFlow()

    private val darkModeBus: MutableStateFlow<Boolean?> = MutableStateFlow(
        null
    )

    val darkModeFlow: StateFlow<Boolean?>
        get() = darkModeBus.asStateFlow()

    init {
        tokenBus.value = settings.getStringOrNull(ACCESS_TOKEN_KEY)
        darkModeBus.value = settings.getBooleanOrNull(DARK_MODE_KEY)
    }

    suspend fun getString(key: String): String? = withContext(Dispatchers.IO) {
        settings.getStringOrNull(key)
    }

    suspend fun putString(key: String, value: String) = withContext(Dispatchers.IO) {
        settings.putString(key, value)
    }

    suspend fun putLong(key: String, value: Long) = withContext(Dispatchers.IO) {
        settings.putLong(key, value)
    }

    suspend fun getLong(key: String) = withContext(Dispatchers.IO) {
        settings.getLongOrNull(key)
    }

    suspend fun setDarkMode(value: Boolean) = withContext(Dispatchers.IO) {
        settings.putBoolean(DARK_MODE_KEY, value)
        darkModeBus.value = value
    }

    suspend fun putAccessToken(value: String?) = withContext(Dispatchers.IO) {
        if (value == null)
            settings.remove(ACCESS_TOKEN_KEY)
        else
            settings.putString(ACCESS_TOKEN_KEY, value)
        tokenBus.value = value
    }

    suspend fun getAccessToken(): String? = getString(ACCESS_TOKEN_KEY)

    private companion object {
        const val ACCESS_TOKEN_KEY = "access_token"
        const val DARK_MODE_KEY = "dark_mode"
    }
}

