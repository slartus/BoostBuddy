package ru.slartus.boostbuddy.components.qrlogin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.kodein.di.bindSingleton
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.log.CompositeLogger
import ru.slartus.boostbuddy.data.log.Logger
import ru.slartus.boostbuddy.data.pairing.PairingPayload
import ru.slartus.boostbuddy.data.pairing.PairingReceiver
import ru.slartus.boostbuddy.data.pairing.PairingReceiverEvent
import ru.slartus.boostbuddy.data.pairing.PairingReceiverException
import ru.slartus.boostbuddy.data.pairing.PairingSession
import ru.slartus.boostbuddy.data.repositories.AuthSession
import ru.slartus.boostbuddy.data.repositories.Profile
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class QrLoginControllerTest {

    private val previous = AuthSession(null, null, null, "receiver-device")
    private val payload = PairingPayload(
        accessToken = "donor-access",
        refreshToken = "donor-refresh",
        expiresAt = 1_900_000_000L,
        deviceId = "donor-device",
        source = PairingPayload.SOURCE_WEB,
        iat = 1_700_000_000L,
    )
    private val donorSession = AuthSession("donor-access", "donor-refresh", 1_900_000_000L, "donor-device")

    private val receiver = FakeReceiver()
    private val gateway = FakeGateway(previous)
    private var now = 1_000_000L
    private var logined = 0

    @BeforeTest
    fun setUp() {
        Inject.createDependenciesTree { bindSingleton<Logger> { CompositeLogger(emptyList()) } }
    }

    @Test
    fun received_appliesSessionAndShowsConfirmWithoutRefresh() = test { controller ->
        val qr = startAndAwaitQr(controller)

        receiver.emit(PairingReceiverEvent.Received(qr.sid, payload))

        val confirm = controller.state.await<QrLoginViewState.Confirm>()
        assertEquals("Donor", confirm.profileName)
        assertEquals("https://avatar", confirm.avatarUrl)
        assertEquals(donorSession, gateway.session)
        assertEquals(0, gateway.commitCalls)
        assertEquals(1, gateway.profileCalls)
    }

    @Test
    fun profileFailure_rollsBackSession() = test { controller ->
        gateway.profileResult = Result.failure(IllegalStateException("401"))
        val qr = startAndAwaitQr(controller)

        receiver.emit(PairingReceiverEvent.Received(qr.sid, payload))

        assertEquals(QrLoginErrorKind.ProfileFailed, controller.state.await<QrLoginViewState.Error>().kind)
        assertEquals(previous, gateway.session)
        assertEquals(0, gateway.commitCalls)
    }

    @Test
    fun confirm_refreshSucceeds_completesLogin() = test { controller ->
        awaitConfirm(controller)

        controller.onConfirm()

        controller.state.await<QrLoginViewState.Success>()
        assertEquals(1, gateway.commitCalls)
        assertEquals(1, logined)
        assertEquals("rotated-access", gateway.session.accessToken)
        assertEquals("donor-device", gateway.session.deviceId)
    }

    @Test
    fun confirm_refreshRejected_rollsBackSession() = test { controller ->
        gateway.commitResults += QrLoginCommitResult.Rejected
        awaitConfirm(controller)

        controller.onConfirm()

        assertEquals(QrLoginErrorKind.RefreshRejected, controller.state.await<QrLoginViewState.Error>().kind)
        assertEquals(previous, gateway.session)
        assertFalse(gateway.hasPending)
        assertEquals(0, logined)
    }

    @Test
    fun confirm_networkError_keepsPendingAndRetryRecommits() = test { controller ->
        gateway.commitResults += QrLoginCommitResult.NetworkError
        awaitConfirm(controller)

        controller.onConfirm()

        assertEquals(QrLoginErrorKind.RefreshNetwork, controller.state.await<QrLoginViewState.Error>().kind)
        assertTrue(gateway.hasPending)
        assertEquals(donorSession, gateway.session)

        controller.onRetry()

        controller.state.await<QrLoginViewState.Success>()
        assertEquals(2, gateway.commitCalls)
        assertEquals(1, receiver.startCalls)
        assertEquals(1, logined)
    }

    @Test
    fun refreshQr_afterNetworkError_rollsBackPending() = test { controller ->
        gateway.commitResults += QrLoginCommitResult.NetworkError
        awaitConfirm(controller)
        controller.onConfirm()
        controller.state.await<QrLoginViewState.Error>()

        controller.onRefreshQr()

        controller.state.await<QrLoginViewState.ShowQr> { it.pageUrl.endsWith("sid-2") }
        assertEquals(previous, gateway.session)
        assertFalse(gateway.hasPending)
    }

    @Test
    fun doubleBack_navigatesOnce() = test { controller ->
        awaitConfirm(controller)
        var navigations = 0

        controller.onBack { navigations++ }
        controller.onBack { navigations++ }

        withTimeout(TIMEOUT) { while (navigations == 0) yield() }
        repeat(5) { yield() }
        assertEquals(1, navigations)
        assertEquals(previous, gateway.session)
    }

    @Test
    fun cancel_rollsBackAndShowsNewQr() = test { controller ->
        awaitConfirm(controller)

        controller.onCancel()

        val qr = controller.state.await<QrLoginViewState.ShowQr> { it.pageUrl.endsWith("sid-2") }
        assertEquals("192.168.1.10", qr.host)
        assertEquals(previous, gateway.session)
        assertEquals(2, receiver.startCalls)
        assertEquals(0, gateway.commitCalls)
    }

    @Test
    fun back_onConfirm_rollsBackAndNavigates() = test { controller ->
        awaitConfirm(controller)
        var navigated = false

        controller.onBack { navigated = true }

        withTimeout(TIMEOUT) { while (!navigated) yield() }
        assertEquals(previous, gateway.session)
        assertEquals(0, gateway.commitCalls)
    }

    @Test
    fun destroy_onConfirm_rollsBackSession() = test { controller ->
        awaitConfirm(controller)

        controller.onDestroy()

        withTimeout(TIMEOUT) { while (gateway.session != previous) yield() }
        assertEquals(0, gateway.commitCalls)
    }

    @Test
    fun stoppedStartFailure_isSilent() = test { controller ->
        receiver.startResults += Result.failure(PairingReceiverException.Stopped())

        controller.onStart()
        withTimeout(TIMEOUT) { while (receiver.startCalls == 0) yield() }
        yield()

        assertIs<QrLoginViewState.Starting>(controller.state.value)
    }

    @Test
    fun noLanStartFailure_showsError() = test { controller ->
        receiver.startResults += Result.failure(PairingReceiverException.NoLan())

        controller.onStart()

        assertEquals(QrLoginErrorKind.NoLan, controller.state.await<QrLoginViewState.Error>().kind)
    }

    @Test
    fun foreignSid_isIgnored() = test { controller ->
        val qr = startAndAwaitQr(controller)

        receiver.emit(PairingReceiverEvent.Received("foreign-sid", payload))
        repeat(5) { yield() }

        assertIs<QrLoginViewState.ShowQr>(controller.state.value)
        assertEquals(previous, gateway.session)

        receiver.emit(PairingReceiverEvent.Received(qr.sid, payload))
        controller.state.await<QrLoginViewState.Confirm>()
    }

    @Test
    fun ttlElapsed_showsExpiredAndStopsReceiver() = test { controller ->
        receiver.ttlMs = 0

        controller.onStart()

        assertEquals(QrLoginErrorKind.Expired, controller.state.await<QrLoginViewState.Error>().kind)
        assertTrue(receiver.stopCalls > 0)
    }

    @Test
    fun tooManyAttempts_showsError() = test { controller ->
        val qr = startAndAwaitQr(controller)

        receiver.emit(PairingReceiverEvent.TooManyAttempts(qr.sid))

        assertEquals(QrLoginErrorKind.TooManyAttempts, controller.state.await<QrLoginViewState.Error>().kind)
    }

    @Test
    fun secondsLeft_followsExpiry() = test { controller ->
        receiver.ttlMs = 90_500

        controller.onStart()

        assertEquals(91, controller.state.await<QrLoginViewState.ShowQr>().secondsLeft)
    }

    @Test
    fun stopAndStart_restartsReceiverWithNewQr() = test { controller ->
        startAndAwaitQr(controller)

        controller.onStop()
        assertIs<QrLoginViewState.Starting>(controller.state.value)
        assertEquals(1, receiver.stopCalls)

        controller.onStart()
        controller.state.await<QrLoginViewState.ShowQr> { it.pageUrl.endsWith("sid-2") }
    }

    @Test
    fun retryAfterExpired_showsNewQr() = test { controller ->
        val qr = startAndAwaitQr(controller)
        receiver.emit(PairingReceiverEvent.Expired(qr.sid))
        controller.state.await<QrLoginViewState.Error>()

        controller.onRetry()

        controller.state.await<QrLoginViewState.ShowQr> { it.pageUrl.endsWith("sid-2") }
    }

    private suspend fun startAndAwaitQr(controller: QrLoginController): FakeQr {
        controller.onStart()
        val state = controller.state.await<QrLoginViewState.ShowQr>()
        return FakeQr(sid = state.pageUrl.substringAfterLast('#'))
    }

    private suspend fun awaitConfirm(controller: QrLoginController) {
        val qr = startAndAwaitQr(controller)
        receiver.emit(PairingReceiverEvent.Received(qr.sid, payload))
        controller.state.await<QrLoginViewState.Confirm>()
    }

    private fun test(block: suspend CoroutineScope.(QrLoginController) -> Unit) = runBlocking {
        val scope = CoroutineScope(coroutineContext[ContinuationInterceptor]!! + SupervisorJob())
        val controller = QrLoginController(
            scope = scope,
            appScope = scope,
            receiverProvider = { receiver },
            gateway = gateway,
            nowMs = { now },
            onLogined = { logined++ },
        )
        try {
            block(controller)
        } finally {
            scope.cancel()
        }
    }

    private data class FakeQr(val sid: String)

    private inner class FakeReceiver : PairingReceiver {
        private val flow = MutableSharedFlow<PairingReceiverEvent>(replay = 1)
        val startResults = ArrayDeque<Result<PairingSession>>()
        var startCalls = 0
        var stopCalls = 0
        var ttlMs = 120_000L

        override val events: Flow<PairingReceiverEvent> = flow

        override suspend fun start(): Result<PairingSession> {
            startCalls++
            flow.resetReplayCache()
            return startResults.removeFirstOrNull() ?: Result.success(
                PairingSession(
                    pageUrl = "http://192.168.1.10:8080/p#sid-$startCalls",
                    host = "192.168.1.10",
                    port = 8080,
                    sid = "sid-$startCalls",
                    expiresAtMs = now + ttlMs,
                )
            )
        }

        override fun stop() {
            stopCalls++
        }

        suspend fun emit(event: PairingReceiverEvent) = flow.emit(event)
    }

    private class FakeGateway(var session: AuthSession) : QrLoginSessionGateway {
        var profileResult: Result<Profile> = Result.success(Profile("id", null, "Donor", "https://avatar"))
        var commitResults = ArrayDeque<QrLoginCommitResult>()
        var rollbackSnapshot: AuthSession? = null
        var commitCalls = 0
        var profileCalls = 0

        val hasPending: Boolean get() = rollbackSnapshot != null

        override suspend fun beginPendingSession(session: AuthSession) {
            if (rollbackSnapshot == null) rollbackSnapshot = this.session
            this.session = session
        }

        override suspend fun rollbackPendingSession() {
            rollbackSnapshot?.let { session = it }
            rollbackSnapshot = null
        }

        override suspend fun fetchProfileWithoutRefresh(): Result<Profile> {
            profileCalls++
            return profileResult
        }

        override suspend fun commitPendingSession(): QrLoginCommitResult {
            commitCalls++
            val result = commitResults.removeFirstOrNull() ?: QrLoginCommitResult.Committed
            if (result == QrLoginCommitResult.Committed) {
                session = session.copy(accessToken = "rotated-access", refreshToken = "rotated-refresh")
                rollbackSnapshot = null
            }
            return result
        }
    }

    private companion object {
        const val TIMEOUT = 2_000L
    }
}

internal suspend inline fun <reified T> StateFlow<*>.await(crossinline predicate: (T) -> Boolean = { true }): T =
    withTimeout(2_000L) { first { it is T && predicate(it) } } as T
