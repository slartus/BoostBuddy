package ru.slartus.boostbuddy.data.repositories

import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthSettingsMigrationTest {

    @Test
    fun migrate_movesSessionKeysToTarget() {
        val legacy = InMemorySettings(legacySession() + ("dark_mode" to true))
        val target = InMemorySettings()

        SettingsRepository.migrateAuthSettings(legacy, target)

        assertEquals(legacySession(), target.values)
        assertEquals(mapOf<String, Any>("dark_mode" to true), legacy.values)
    }

    @Test
    fun migrate_isIdempotent() {
        val legacy = InMemorySettings(legacySession())
        val target = InMemorySettings()

        SettingsRepository.migrateAuthSettings(legacy, target)
        SettingsRepository.migrateAuthSettings(legacy, target)

        assertEquals(legacySession(), target.values)
        assertTrue(legacy.values.isEmpty())
    }

    @Test
    fun migrate_rerunDoesNotOverwriteNewerSession() {
        val legacy = InMemorySettings(legacySession())
        val target = InMemorySettings()
        SettingsRepository.migrateAuthSettings(legacy, target)
        target.putString("refresh_token", "rotated")

        SettingsRepository.migrateAuthSettings(legacy, target)

        assertEquals("rotated", target.getStringOrNull("refresh_token"))
    }

    @Test
    fun migrate_withoutLegacyKeys_leavesTargetUntouched() {
        val legacy = InMemorySettings(mapOf("dark_mode" to false))
        val target = InMemorySettings(mapOf("access_token" to "current"))

        SettingsRepository.migrateAuthSettings(legacy, target)

        assertEquals(mapOf<String, Any>("access_token" to "current"), target.values)
        assertEquals(mapOf<String, Any>("dark_mode" to false), legacy.values)
    }

    @Test
    fun migrate_afterInterruptedRun_keepsTargetValuesAndClearsLegacy() {
        val legacy = InMemorySettings(legacySession())
        val target = InMemorySettings(
            mapOf("access_token" to "new-access", "refresh_token" to "new-refresh")
        )

        SettingsRepository.migrateAuthSettings(legacy, target)

        assertEquals("new-access", target.getStringOrNull("access_token"))
        assertEquals("new-refresh", target.getStringOrNull("refresh_token"))
        assertEquals("device", target.getStringOrNull("device_id"))
        assertEquals(1_700_000_000L, target.getLongOrNull("token_expires_at"))
        assertTrue(legacy.values.isEmpty())
    }

    @Test
    fun migrate_sameStorage_keepsSession() {
        val settings = InMemorySettings(legacySession())

        SettingsRepository.migrateAuthSettings(settings, settings)

        assertEquals(legacySession(), settings.values)
    }

    @Test
    fun migrate_writesTargetBeforeRemovingLegacy() {
        val legacy = InMemorySettings(legacySession())
        val target = InMemorySettings()
        legacy.onRemove = { key -> assertTrue(target.hasKey(key), "$key removed before copy") }

        SettingsRepository.migrateAuthSettings(legacy, target)

        assertFalse(legacy.hasKey("refresh_token"))
    }

    private fun legacySession(): Map<String, Any> = mapOf(
        "access_token" to "access",
        "refresh_token" to "refresh",
        "device_id" to "device",
        "token_expires_at" to 1_700_000_000L,
    )
}

internal class InMemorySettings(initial: Map<String, Any> = emptyMap()) : Settings {
    val values = initial.toMutableMap()
    var onRemove: (String) -> Unit = {}

    override val keys: Set<String> get() = values.keys.toSet()
    override val size: Int get() = values.size
    override fun clear() = values.clear()
    override fun remove(key: String) {
        onRemove(key)
        values.remove(key)
    }
    override fun hasKey(key: String): Boolean = key in values
    override fun putInt(key: String, value: Int) { values[key] = value }
    override fun getInt(key: String, defaultValue: Int): Int = getIntOrNull(key) ?: defaultValue
    override fun getIntOrNull(key: String): Int? = values[key] as? Int
    override fun putLong(key: String, value: Long) { values[key] = value }
    override fun getLong(key: String, defaultValue: Long): Long = getLongOrNull(key) ?: defaultValue
    override fun getLongOrNull(key: String): Long? = values[key] as? Long
    override fun putString(key: String, value: String) { values[key] = value }
    override fun getString(key: String, defaultValue: String): String = getStringOrNull(key) ?: defaultValue
    override fun getStringOrNull(key: String): String? = values[key] as? String
    override fun putFloat(key: String, value: Float) { values[key] = value }
    override fun getFloat(key: String, defaultValue: Float): Float = getFloatOrNull(key) ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = values[key] as? Float
    override fun putDouble(key: String, value: Double) { values[key] = value }
    override fun getDouble(key: String, defaultValue: Double): Double = getDoubleOrNull(key) ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = values[key] as? Double
    override fun putBoolean(key: String, value: Boolean) { values[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = getBooleanOrNull(key) ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = values[key] as? Boolean
}
