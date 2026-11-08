package ru.slartus.boostbuddy.data.repositories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PendingSessionTest {

    private val authStore = InMemorySettings()
    private val previous = AuthSession(null, null, null, "receiver-device")
    private val donor = AuthSession("donor-access", "donor-refresh", 1_900_000_000L, "donor-device")
    private val rotated = AuthSession("rotated-access", "rotated-refresh", 1_950_000_000L, "donor-device")

    private fun repository() = SettingsRepository(
        settings = RecordingObservableSettings(InMemorySettings()),
        authSettings = RecordingObservableSettings(authStore),
        coroutineScope = CoroutineScope(Job()),
    )

    @Test
    fun begin_writesNewSessionAndMarksPending() = runBlocking {
        val repository = repository()
        repository.replaceSession(previous.accessToken, previous.refreshToken, previous.expiresAt, previous.deviceId)

        repository.beginPendingSession(donor)

        assertEquals(donor, repository.getSession())
        assertTrue(repository.hasPendingSession())
    }

    @Test
    fun restartWhilePending_rollsBackToPreviousSession() = runBlocking {
        repository().apply {
            replaceSession(previous.accessToken, previous.refreshToken, previous.expiresAt, previous.deviceId)
            beginPendingSession(donor)
        }

        val restarted = repository()
        restarted.recoverPendingSession()

        assertEquals(previous, restarted.getSession())
        assertFalse(restarted.hasPendingSession())
        assertTrue(authStore.keys.none { it.startsWith("pending_") })
    }

    @Test
    fun rollback_restoresPreviousSession() = runBlocking {
        val repository = repository()
        repository.replaceSession(previous.accessToken, previous.refreshToken, previous.expiresAt, previous.deviceId)
        repository.beginPendingSession(donor)

        assertTrue(repository.rollbackPendingSessionIfAny())

        assertEquals(previous, repository.getSession())
        assertFalse(repository.hasPendingSession())
        assertFalse(repository.rollbackPendingSessionIfAny())
    }

    @Test
    fun secondBegin_keepsOriginalRollbackSnapshot() = runBlocking {
        val repository = repository()
        repository.replaceSession(previous.accessToken, previous.refreshToken, previous.expiresAt, previous.deviceId)
        repository.beginPendingSession(donor)
        repository.beginPendingSession(donor.copy(accessToken = "other"))

        repository.rollbackPendingSessionIfAny()

        assertEquals(previous, repository.getSession())
    }

    @Test
    fun commit_writesRotatedSessionAndClearsPending() = runBlocking {
        val repository = repository()
        repository.beginPendingSession(donor)

        repository.commitPendingSession(rotated)

        assertEquals(rotated, repository.getSession())
        assertFalse(repository.hasPendingSession())
        assertTrue(authStore.keys.none { it.startsWith("pending_") })

        val restarted = repository()
        restarted.recoverPendingSession()
        assertEquals(rotated, restarted.getSession())
    }

    @Test
    fun restartDuringCommit_rollsForwardToRotatedSession() = runBlocking {
        repository().beginPendingSession(donor)
        authStore.putString("pending_commit_access_token", "rotated-access")
        authStore.putString("pending_commit_refresh_token", "rotated-refresh")
        authStore.putLong("pending_commit_token_expires_at", 1_950_000_000L)
        authStore.putString("pending_commit_device_id", "donor-device")
        authStore.putString("pending_session_state", "committing")

        val restarted = repository()
        restarted.recoverPendingSession()

        assertEquals(rotated, restarted.getSession())
        assertFalse(restarted.hasPendingSession())
    }
}
