package ru.slartus.boostbuddy.data.repositories

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getBooleanOrNullFlow
import com.russhwolf.settings.coroutines.getStringOrNullStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(ExperimentalSettingsApi::class)
internal class SettingsRepository(
    private val settings: ObservableSettings,
    private val coroutineScope: CoroutineScope,
) {
    private val locker = Mutex()

    val tokenFlow: StateFlow<String?>
        get() = settings.getStringOrNullStateFlow(coroutineScope, ACCESS_TOKEN_KEY)

    val appSettingsFlow: Flow<AppSettings>
        get() = combine(
            settings.getBooleanOrNullFlow(DARK_MODE_KEY),
            settings.getBooleanFlow(SYSTEM_PLAYER_KEY, false),
            settings.getBooleanFlow(DEBUG_LOG, false)
        ) { darkMode, systemPlayer, debugLog ->
            AppSettings(
                isDarkMode = darkMode,
                useSystemVideoPlayer = systemPlayer,
                debugLog = debugLog
            )
        }

    suspend fun getSettings(): AppSettings = withContext(Dispatchers.IO) {
        AppSettings(
            isDarkMode = settings.getBooleanOrNull(DARK_MODE_KEY),
            useSystemVideoPlayer = settings.getBoolean(SYSTEM_PLAYER_KEY, false),
            debugLog = settings.getBoolean(DEBUG_LOG, false)
        )
    }

    suspend fun setDarkMode(value: Boolean) = withContext(Dispatchers.IO) {
        putBoolean(DARK_MODE_KEY, value)
    }

    suspend fun setUseSystemVideoPlayer(value: Boolean) = withContext(Dispatchers.IO) {
        putBoolean(SYSTEM_PLAYER_KEY, value)
    }

    suspend fun setDebugLog(value: Boolean) = withContext(Dispatchers.IO) {
        putBoolean(DEBUG_LOG, value)
    }

    suspend fun putAccessToken(value: String?) = withContext(Dispatchers.IO) {
        if (value == null)
            remove(ACCESS_TOKEN_KEY)
        else
            putString(ACCESS_TOKEN_KEY, value)
    }

    suspend fun getAccessToken(): String? = getString(ACCESS_TOKEN_KEY)

    suspend fun setLastDonateNotifyVersion(version: String) =
        putString(LAST_DONATE_NOTIFY_VERSION_KEY, version)

    suspend fun getLastDonateNotifyVersion(): String? =
        getString(LAST_DONATE_NOTIFY_VERSION_KEY)

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
        const val DEBUG_LOG = "debug_log"
        const val LAST_DONATE_NOTIFY_VERSION_KEY = "LAST_DONATE_NOTIFY_VERSION_KEY"
    }
}

data class AppSettings(
    val isDarkMode: Boolean?,
    val useSystemVideoPlayer: Boolean,
    val debugLog: Boolean
) {
    companion object {
        val Default: AppSettings =
            AppSettings(isDarkMode = null, useSystemVideoPlayer = false, debugLog = false)
    }
}

