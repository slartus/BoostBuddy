package ru.slartus.boostbuddy.data.repositories

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getBooleanOrNullFlow
import com.russhwolf.settings.coroutines.getFloatFlow
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import com.russhwolf.settings.coroutines.getStringOrNullStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.slartus.boostbuddy.data.repositories.models.VideoQuality
import ru.slartus.boostbuddy.data.repositories.models.VideoScaleMode
import kotlin.random.Random

@OptIn(ExperimentalSettingsApi::class)
internal class SettingsRepository(
    private val settings: ObservableSettings,
    private val authSettings: ObservableSettings,
    private val coroutineScope: CoroutineScope,
) {
    private val locker = Mutex()

    val tokenFlow: StateFlow<String?>
        get() = authSettings.getStringOrNullStateFlow(coroutineScope, ACCESS_TOKEN_KEY)

    val appSettingsFlow: Flow<AppSettings>
        get() = combine(
            settings.getBooleanOrNullFlow(DARK_MODE_KEY),
            settings.getBooleanFlow(SYSTEM_PLAYER_KEY, false),
            settings.getBooleanFlow(DEBUG_LOG, false),
            settings.getStringOrNullFlow(PREFERRED_QUALITY_KEY).map { it.toVideoQualityOrNull() },
            settings.getFloatFlow(PREFERRED_PLAYBACK_SPEED_KEY, DEFAULT_PLAYBACK_SPEED)
        ) { darkMode, systemPlayer, debugLog, preferredQuality, preferredPlaybackSpeed ->
            AppSettings(
                isDarkMode = darkMode,
                useSystemVideoPlayer = systemPlayer,
                debugLog = debugLog,
                preferredQuality = preferredQuality,
                preferredPlaybackSpeed = preferredPlaybackSpeed,
                videoScaleMode = VideoScaleMode.Default,
            )
        }.combine(
            settings.getStringOrNullFlow(VIDEO_SCALE_MODE_KEY).map { it.toVideoScaleMode() }
        ) { appSettings, videoScaleMode ->
            appSettings.copy(videoScaleMode = videoScaleMode)
        }

    suspend fun getSettings(): AppSettings = withContext(Dispatchers.IO) {
        AppSettings(
            isDarkMode = settings.getBooleanOrNull(DARK_MODE_KEY),
            useSystemVideoPlayer = settings.getBoolean(SYSTEM_PLAYER_KEY, false),
            debugLog = settings.getBoolean(DEBUG_LOG, false),
            preferredQuality = settings.getStringOrNull(PREFERRED_QUALITY_KEY)
                .toVideoQualityOrNull(),
            preferredPlaybackSpeed = settings.getFloat(
                PREFERRED_PLAYBACK_SPEED_KEY,
                DEFAULT_PLAYBACK_SPEED
            ),
            videoScaleMode = settings.getStringOrNull(VIDEO_SCALE_MODE_KEY).toVideoScaleMode(),
        )
    }

    suspend fun setDarkMode(value: Boolean) = withContext(Dispatchers.IO) {
        putBoolean(DARK_MODE_KEY, value)
    }

    suspend fun setUseSystemVideoPlayer(value: Boolean) = withContext(Dispatchers.IO) {
        putBoolean(SYSTEM_PLAYER_KEY, value)
    }

    suspend fun setPreferredQuality(value: VideoQuality?) = withContext(Dispatchers.IO) {
        if (value == null) {
            remove(PREFERRED_QUALITY_KEY)
        } else {
            putString(PREFERRED_QUALITY_KEY, value.name)
        }
    }

    suspend fun setPreferredPlaybackSpeed(value: Float) = withContext(Dispatchers.IO) {
        putFloat(PREFERRED_PLAYBACK_SPEED_KEY, value)
    }

    suspend fun setVideoScaleMode(value: VideoScaleMode) = withContext(Dispatchers.IO) {
        putString(VIDEO_SCALE_MODE_KEY, value.name)
    }

    suspend fun setDebugLog(value: Boolean) = withContext(Dispatchers.IO) {
        putBoolean(DEBUG_LOG, value)
    }

    suspend fun putAccessToken(value: String?) = withContext(Dispatchers.IO) {
        if (value == null)
            remove(ACCESS_TOKEN_KEY, authSettings)
        else
            putString(ACCESS_TOKEN_KEY, value, authSettings)
    }

    suspend fun getAccessToken(): String? = getString(ACCESS_TOKEN_KEY, authSettings)

    suspend fun putRefreshToken(value: String?) = withContext(Dispatchers.IO) {
        if (value == null)
            remove(REFRESH_TOKEN_KEY, authSettings)
        else
            putString(REFRESH_TOKEN_KEY, value, authSettings)
    }

    suspend fun getRefreshToken(): String? = getString(REFRESH_TOKEN_KEY, authSettings)

    suspend fun putTokenExpiresAt(value: Long?) = withContext(Dispatchers.IO) {
        if (value == null)
            remove(TOKEN_EXPIRES_AT_KEY, authSettings)
        else
            putLong(TOKEN_EXPIRES_AT_KEY, value, authSettings)
    }

    suspend fun getTokenExpiresAt(): Long? = getLong(TOKEN_EXPIRES_AT_KEY, authSettings)

    suspend fun getOrCreateDeviceId(): String = withContext(Dispatchers.IO) {
        locker.withLock {
            val existing = authSettings.getStringOrNull(DEVICE_ID_KEY)
            if (existing != null) return@withLock existing
            val generated = generateDeviceId()
            authSettings.putString(DEVICE_ID_KEY, generated)
            generated
        }
    }

    suspend fun putDeviceId(value: String) = putString(DEVICE_ID_KEY, value, authSettings)

    suspend fun getDeviceId(): String? = getString(DEVICE_ID_KEY, authSettings)

    suspend fun replaceSession(
        accessToken: String?,
        refreshToken: String?,
        expiresAt: Long?,
        deviceId: String?,
    ) = withContext(Dispatchers.IO) {
        locker.withLock {
            authSettings.putOrRemove(DEVICE_ID_KEY, deviceId, Settings::putString)
            authSettings.putOrRemove(TOKEN_EXPIRES_AT_KEY, expiresAt, Settings::putLong)
            authSettings.putOrRemove(REFRESH_TOKEN_KEY, refreshToken, Settings::putString)
            authSettings.putOrRemove(ACCESS_TOKEN_KEY, accessToken, Settings::putString)
        }
    }

    suspend fun getSession(): AuthSession = withContext(Dispatchers.IO) {
        locker.withLock { authSettings.readSession(SESSION_KEYS) }
    }

    suspend fun hasPendingSession(): Boolean = withContext(Dispatchers.IO) {
        locker.withLock { authSettings.getStringOrNull(PENDING_STATE_KEY) != null }
    }

    suspend fun beginPendingSession(session: AuthSession) = withContext(Dispatchers.IO) {
        locker.withLock {
            if (authSettings.getStringOrNull(PENDING_STATE_KEY) == null) {
                authSettings.writeSession(PENDING_ROLLBACK_KEYS, authSettings.readSession(SESSION_KEYS))
                authSettings.putString(PENDING_STATE_KEY, PENDING_STATE_PENDING)
            }
            authSettings.writeSession(SESSION_KEYS, session)
        }
    }

    suspend fun confirmPendingSession() = withContext(Dispatchers.IO) {
        locker.withLock {
            if (authSettings.getStringOrNull(PENDING_STATE_KEY) == PENDING_STATE_PENDING) {
                authSettings.putString(PENDING_STATE_KEY, PENDING_STATE_CONFIRMED)
            }
        }
    }

    suspend fun rollbackPendingSessionIfAny(): Boolean = withContext(Dispatchers.IO) {
        locker.withLock {
            val state = authSettings.getStringOrNull(PENDING_STATE_KEY)
            if (state != PENDING_STATE_PENDING && state != PENDING_STATE_CONFIRMED) return@withLock false
            authSettings.writeSession(SESSION_KEYS, authSettings.readSession(PENDING_ROLLBACK_KEYS))
            authSettings.clearPendingSession()
            true
        }
    }

    suspend fun commitPendingSession(session: AuthSession) = withContext(Dispatchers.IO) {
        locker.withLock {
            authSettings.writeSession(PENDING_COMMIT_KEYS, session)
            authSettings.putString(PENDING_STATE_KEY, PENDING_STATE_COMMITTING)
            authSettings.writeSession(SESSION_KEYS, session)
            authSettings.clearPendingSession()
        }
    }

    suspend fun recoverPendingSession() = withContext(Dispatchers.IO) {
        locker.withLock {
            when (authSettings.getStringOrNull(PENDING_STATE_KEY)) {
                null -> return@withLock
                PENDING_STATE_COMMITTING ->
                    authSettings.writeSession(SESSION_KEYS, authSettings.readSession(PENDING_COMMIT_KEYS))

                PENDING_STATE_CONFIRMED -> Unit

                else -> authSettings.writeSession(SESSION_KEYS, authSettings.readSession(PENDING_ROLLBACK_KEYS))
            }
            authSettings.clearPendingSession()
        }
    }

    private fun Settings.readSession(keys: SessionKeys): AuthSession = AuthSession(
        accessToken = getStringOrNull(keys.accessToken),
        refreshToken = getStringOrNull(keys.refreshToken),
        expiresAt = getLongOrNull(keys.expiresAt),
        deviceId = getStringOrNull(keys.deviceId),
    )

    private fun Settings.writeSession(keys: SessionKeys, session: AuthSession) {
        putOrRemove(keys.deviceId, session.deviceId, Settings::putString)
        putOrRemove(keys.expiresAt, session.expiresAt, Settings::putLong)
        putOrRemove(keys.refreshToken, session.refreshToken, Settings::putString)
        putOrRemove(keys.accessToken, session.accessToken, Settings::putString)
    }

    private fun Settings.clearPendingSession() {
        remove(PENDING_STATE_KEY)
        (PENDING_ROLLBACK_KEYS.all + PENDING_COMMIT_KEYS.all).forEach(::remove)
    }

    private class SessionKeys(
        val accessToken: String,
        val refreshToken: String,
        val expiresAt: String,
        val deviceId: String,
    ) {
        val all: List<String> get() = listOf(accessToken, refreshToken, expiresAt, deviceId)
    }

    suspend fun clearSessionWithNewDeviceId() =
        replaceSession(accessToken = null, refreshToken = null, expiresAt = null, deviceId = generateDeviceId())

    private fun <T : Any> Settings.putOrRemove(key: String, value: T?, put: Settings.(String, T) -> Unit) {
        if (value == null) remove(key) else put(key, value)
    }

    suspend fun isWebDeviceIdMigrated(): Boolean = getBoolean(WEB_DEVICE_ID_MIGRATED_KEY, false)

    suspend fun setWebDeviceIdMigrated() = putBoolean(WEB_DEVICE_ID_MIGRATED_KEY, true)

    private fun generateDeviceId(): String {
        val bytes = ByteArray(16).also { Random.nextBytes(it) }
        bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
        fun ByteArray.hex(from: Int, to: Int) = buildString {
            for (i in from until to) {
                val v = this@hex[i].toInt() and 0xff
                append(v.toString(16).padStart(2, '0'))
            }
        }
        return buildString {
            append(bytes.hex(0, 4)); append('-')
            append(bytes.hex(4, 6)); append('-')
            append(bytes.hex(6, 8)); append('-')
            append(bytes.hex(8, 10)); append('-')
            append(bytes.hex(10, 16))
        }
    }

    suspend fun setLastDonateNotifyVersion(version: String) =
        putString(LAST_DONATE_NOTIFY_VERSION_KEY, version)

    suspend fun getLastDonateNotifyVersion(): String? =
        getString(LAST_DONATE_NOTIFY_VERSION_KEY)

    private suspend fun getString(key: String, store: Settings = settings): String? = withContext(Dispatchers.IO) {
        locker.withLock {
            store.getStringOrNull(key)
        }
    }

    private suspend fun putString(key: String, value: String, store: Settings = settings) = withContext(Dispatchers.IO) {
        locker.withLock {
            store.putString(key, value)
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

    private suspend fun putLong(key: String, value: Long, store: Settings = settings) = withContext(Dispatchers.IO) {
        locker.withLock {
            store.putLong(key, value)
        }
    }

    private suspend fun putFloat(key: String, value: Float) = withContext(Dispatchers.IO) {
        locker.withLock {
            settings.putFloat(key, value)
        }
    }

    private suspend fun getLong(key: String, store: Settings = settings) = withContext(Dispatchers.IO) {
        locker.withLock {
            store.getLongOrNull(key)
        }
    }

    private suspend fun remove(key: String, store: Settings = settings) = withContext(Dispatchers.IO) {
        locker.withLock {
            store.remove(key)
        }
    }

    suspend fun getDonationPromptVersion(): String? =
        settings.getString(KEY_DONATION_PROMPT_VERSION, "")

    suspend fun setDonationPromptVersion(version: String) {
        settings.putString(KEY_DONATION_PROMPT_VERSION, version)
    }

    internal companion object {
        fun migrateAuthSettings(legacy: Settings, target: Settings) {
            if (legacy === target) return
            AUTH_STRING_KEYS.forEach { key ->
                val value = legacy.getStringOrNull(key)
                if (value != null && !target.hasKey(key)) target.putString(key, value)
            }
            AUTH_LONG_KEYS.forEach { key ->
                val value = legacy.getLongOrNull(key)
                if (value != null && !target.hasKey(key)) target.putLong(key, value)
            }
            (AUTH_STRING_KEYS + AUTH_LONG_KEYS)
                .filter(legacy::hasKey)
                .forEach(legacy::remove)
        }

        private val AUTH_STRING_KEYS = listOf(ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY, DEVICE_ID_KEY)
        private val AUTH_LONG_KEYS = listOf(TOKEN_EXPIRES_AT_KEY)

        private const val KEY_DONATION_PROMPT_VERSION = "KEY_DONATION_PROMPT_VERSION"
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
        private const val TOKEN_EXPIRES_AT_KEY = "token_expires_at"
        private const val DEVICE_ID_KEY = "device_id"
        private const val PENDING_STATE_KEY = "pending_session_state"
        private const val PENDING_STATE_PENDING = "pending"
        private const val PENDING_STATE_CONFIRMED = "confirmed"
        private const val PENDING_STATE_COMMITTING = "committing"
        private val SESSION_KEYS = SessionKeys(ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY, TOKEN_EXPIRES_AT_KEY, DEVICE_ID_KEY)
        private val PENDING_ROLLBACK_KEYS = SessionKeys(
            accessToken = "pending_rollback_access_token",
            refreshToken = "pending_rollback_refresh_token",
            expiresAt = "pending_rollback_token_expires_at",
            deviceId = "pending_rollback_device_id",
        )
        private val PENDING_COMMIT_KEYS = SessionKeys(
            accessToken = "pending_commit_access_token",
            refreshToken = "pending_commit_refresh_token",
            expiresAt = "pending_commit_token_expires_at",
            deviceId = "pending_commit_device_id",
        )
        private const val WEB_DEVICE_ID_MIGRATED_KEY = "web_device_id_migrated"
        private const val DARK_MODE_KEY = "dark_mode"
        private const val SYSTEM_PLAYER_KEY = "system_player"
        private const val DEBUG_LOG = "debug_log"
        private const val LAST_DONATE_NOTIFY_VERSION_KEY = "LAST_DONATE_NOTIFY_VERSION_KEY"
        private const val PREFERRED_QUALITY_KEY = "preferred_video_quality"
        private const val PREFERRED_PLAYBACK_SPEED_KEY = "preferred_playback_speed"
        private const val DEFAULT_PLAYBACK_SPEED = 1f
        private const val VIDEO_SCALE_MODE_KEY = "video_scale_mode"
    }
}

private fun String?.toVideoQualityOrNull(): VideoQuality? =
    this?.let { name -> VideoQuality.entries.firstOrNull { it.name == name } }

private fun String?.toVideoScaleMode(): VideoScaleMode =
    VideoScaleMode.entries.firstOrNull { it.name == this } ?: VideoScaleMode.Default

data class AppSettings(
    val isDarkMode: Boolean?,
    val useSystemVideoPlayer: Boolean,
    val debugLog: Boolean,
    val preferredQuality: VideoQuality?,
    val preferredPlaybackSpeed: Float,
    val videoScaleMode: VideoScaleMode,
) {
    companion object {
        val Default: AppSettings = AppSettings(
            isDarkMode = null,
            useSystemVideoPlayer = false,
            debugLog = false,
            preferredQuality = null,
            preferredPlaybackSpeed = 1f,
            videoScaleMode = VideoScaleMode.Default,
        )
    }
}

