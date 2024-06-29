package ru.slartus.boostbuddy.data.repositories

import com.russhwolf.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class SettingsRepository(
    private val settings: Settings
) {
    private val locker = Mutex()
    private val tokenBus: MutableStateFlow<String?> = MutableStateFlow(
        null
    )

    val tokenFlow: StateFlow<String?>
        get() = tokenBus.asStateFlow()

    private val appSettingsBus: MutableStateFlow<AppSettings> = MutableStateFlow(
        AppSettings.Default
    )

    val appSettingsFlow: StateFlow<AppSettings>
        get() = appSettingsBus.asStateFlow()

    init {
        tokenBus.value = settings.getStringOrNull(ACCESS_TOKEN_KEY)
        loadAppSettings()
    }

    private fun loadAppSettings() {
        appSettingsBus.value = AppSettings(
            isDarkMode = settings.getBooleanOrNull(DARK_MODE_KEY),
            useSystemVideoPlayer = settings.getBoolean(SYSTEM_PLAYER_KEY, false)
        )
    }

    suspend fun getSettings(): AppSettings = appSettingsBus.value

    suspend fun setDarkMode(value: Boolean) = withContext(Dispatchers.IO) {
        putBoolean(DARK_MODE_KEY, value)
        loadAppSettings()
    }

    suspend fun setUseSystemVideoPlayer(value: Boolean) = withContext(Dispatchers.IO) {
        putBoolean(SYSTEM_PLAYER_KEY, value)
        loadAppSettings()
    }

    suspend fun putAccessToken(value: String?) = withContext(Dispatchers.IO) {
        if (value == null)
            remove(ACCESS_TOKEN_KEY)
        else
            putString(ACCESS_TOKEN_KEY, value)
        tokenBus.value = value
    }

    suspend fun getAccessToken(): String? = getString(ACCESS_TOKEN_KEY)

    private suspend fun getString(key: String): String? = withContext(Dispatchers.IO) {
        locker.withLock {
            settings.getStringOrNull(key)
        }
    }

    private suspend fun putString(key: String, value: String) = withContext(Dispatchers.IO) {
        locker.withLock {
            settings.putString(key, value)
        }
    }

    private suspend fun putBoolean(key: String, value: Boolean) = withContext(Dispatchers.IO) {
        locker.withLock {
            settings.putBoolean(key, value)
        }
    }

    private suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            locker.withLock {
                settings.getBoolean(key, defaultValue)
            }
        }

    private suspend fun getBooleanOrNull(key: String): Boolean? = withContext(Dispatchers.IO) {
        locker.withLock {
            settings.getBooleanOrNull(key)
        }
    }

    private suspend fun putLong(key: String, value: Long) = withContext(Dispatchers.IO) {
        locker.withLock {
            settings.putLong(key, value)
        }
    }

    private suspend fun getLong(key: String) = withContext(Dispatchers.IO) {
        locker.withLock {
            settings.getLongOrNull(key)
        }
    }

    private suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        locker.withLock {
            settings.remove(key)
        }
    }

    private companion object {
        const val ACCESS_TOKEN_KEY = "access_token"
        const val DARK_MODE_KEY = "dark_mode"
        const val SYSTEM_PLAYER_KEY = "system_player"
    }
}

data class AppSettings(val isDarkMode: Boolean?, val useSystemVideoPlayer: Boolean) {
    companion object {
        val Default: AppSettings = AppSettings(isDarkMode = null, useSystemVideoPlayer = false)
    }
}

