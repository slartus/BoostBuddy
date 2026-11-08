package ru.slartus.boostbuddy.data.repositories

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SettingsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ReplaceSessionTest {

    private val auth = RecordingObservableSettings(InMemorySettings())
    private val repository = SettingsRepository(
        settings = RecordingObservableSettings(InMemorySettings()),
        authSettings = auth,
        coroutineScope = CoroutineScope(Job()),
    )

    @Test
    fun replaceSession_writesAllValuesWithAccessTokenLast() = runBlocking {
        repository.replaceSession("access", "refresh", 1_800_000_000L, "device")

        assertEquals(
            mapOf<String, Any>(
                "access_token" to "access",
                "refresh_token" to "refresh",
                "token_expires_at" to 1_800_000_000L,
                "device_id" to "device",
            ),
            auth.delegate.values,
        )
        assertEquals(
            listOf("put:device_id", "put:token_expires_at", "put:refresh_token", "put:access_token"),
            auth.operations,
        )
    }

    @Test
    fun replaceSession_overwritesExistingSession() = runBlocking {
        repository.replaceSession("old-access", "old-refresh", 1L, "old-device")

        repository.replaceSession("new-access", "new-refresh", 2L, "new-device")

        assertEquals("new-access", repository.getAccessToken())
        assertEquals("new-refresh", repository.getRefreshToken())
        assertEquals(2L, repository.getTokenExpiresAt())
        assertEquals("new-device", repository.getDeviceId())
    }

    @Test
    fun replaceSession_nullRemovesKeys() = runBlocking {
        repository.replaceSession("access", "refresh", 1L, "device")
        auth.operations.clear()

        repository.replaceSession(null, null, null, null)

        assertEquals(emptyMap(), auth.delegate.values)
        assertEquals(
            listOf("remove:device_id", "remove:token_expires_at", "remove:refresh_token", "remove:access_token"),
            auth.operations,
        )
    }

    @Test
    fun getSession_readsAllValues() = runBlocking {
        repository.replaceSession("access", "refresh", 1L, "device")

        assertEquals(AuthSession("access", "refresh", 1L, "device"), repository.getSession())
    }

    @Test
    fun clearSessionWithNewDeviceId_removesTokensAndRegeneratesDeviceId() = runBlocking {
        repository.replaceSession("access", "refresh", 1L, "device")

        repository.clearSessionWithNewDeviceId()

        val session = repository.getSession()
        assertEquals(null, session.accessToken)
        assertEquals(null, session.refreshToken)
        assertEquals(null, session.expiresAt)
        assertNotEquals("device", session.deviceId)
        assertTrue(session.deviceId.orEmpty().matches(UUID_REGEX))
    }
}

private val UUID_REGEX = Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")

private class RecordingObservableSettings(
    val delegate: InMemorySettings,
) : ObservableSettings, Settings by delegate {
    val operations = mutableListOf<String>()

    override fun putString(key: String, value: String) {
        operations += "put:$key"
        delegate.putString(key, value)
    }

    override fun putLong(key: String, value: Long) {
        operations += "put:$key"
        delegate.putLong(key, value)
    }

    override fun remove(key: String) {
        operations += "remove:$key"
        delegate.remove(key)
    }

    override fun addIntListener(key: String, defaultValue: Int, callback: (Int) -> Unit): SettingsListener = NoOpListener
    override fun addLongListener(key: String, defaultValue: Long, callback: (Long) -> Unit): SettingsListener = NoOpListener
    override fun addStringListener(key: String, defaultValue: String, callback: (String) -> Unit): SettingsListener = NoOpListener
    override fun addFloatListener(key: String, defaultValue: Float, callback: (Float) -> Unit): SettingsListener = NoOpListener
    override fun addDoubleListener(key: String, defaultValue: Double, callback: (Double) -> Unit): SettingsListener = NoOpListener
    override fun addBooleanListener(key: String, defaultValue: Boolean, callback: (Boolean) -> Unit): SettingsListener = NoOpListener
    override fun addIntOrNullListener(key: String, callback: (Int?) -> Unit): SettingsListener = NoOpListener
    override fun addLongOrNullListener(key: String, callback: (Long?) -> Unit): SettingsListener = NoOpListener
    override fun addStringOrNullListener(key: String, callback: (String?) -> Unit): SettingsListener = NoOpListener
    override fun addFloatOrNullListener(key: String, callback: (Float?) -> Unit): SettingsListener = NoOpListener
    override fun addDoubleOrNullListener(key: String, callback: (Double?) -> Unit): SettingsListener = NoOpListener
    override fun addBooleanOrNullListener(key: String, callback: (Boolean?) -> Unit): SettingsListener = NoOpListener

    private object NoOpListener : SettingsListener {
        override fun deactivate() = Unit
    }
}
