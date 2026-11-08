package ru.slartus.boostbuddy.components.pairsend

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.kodein.di.bindSingleton
import ru.slartus.boostbuddy.components.qrlogin.await
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.log.CompositeLogger
import ru.slartus.boostbuddy.data.log.Logger
import ru.slartus.boostbuddy.data.pairing.PairingEnvelope
import ru.slartus.boostbuddy.data.pairing.PairingPayload
import ru.slartus.boostbuddy.data.pairing.PairingSender
import ru.slartus.boostbuddy.data.pairing.PairingSenderException
import ru.slartus.boostbuddy.data.pairing.PairingTarget
import ru.slartus.boostbuddy.data.repositories.AuthSession
import ru.slartus.boostbuddy.utils.ServerException
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PairSendControllerTest {

    private val target = PairingTarget(
        host = "192.168.1.20",
        port = 8080,
        sid = PairingEnvelope.newSid(),
        key = PairingEnvelope.newKey(),
    )
    private val session = AuthSession("access", "refresh", 1_900_000_000L, "sender-device")
    private val gateway = FakeGateway()
    private val sender = FakeSender()
    private var transferred = 0

    @BeforeTest
    fun setUp() {
        Inject.createDependenciesTree { bindSingleton<Logger> { CompositeLogger(emptyList()) } }
    }

    @Test
    fun initialState_isConfirmWithHost() = test { controller ->
        assertEquals(PairSendViewState.Confirm("192.168.1.20"), controller.state.value)
    }

    @Test
    fun send_success_sendsSealedPayloadAndSignsOutLocally() = test { controller ->
        controller.onSend()

        controller.state.await<PairSendViewState.Done>()
        assertEquals(1, transferred)
        assertEquals(1, gateway.clearCalls)
        assertTrue(gateway.clearedInsideExclusive)
        val sent = sender.sent.single()
        assertEquals(target, sent.first)
        val payload = Json.decodeFromString<PairingPayload>(
            PairingEnvelope.open(target.key, target.sid, sent.second).decodeToString()
        )
        assertEquals("access", payload.accessToken)
        assertEquals("refresh", payload.refreshToken)
        assertEquals(1_900_000_000L, payload.expiresAt)
        assertEquals("sender-device", payload.deviceId)
        assertEquals(PairingPayload.SOURCE_APP, payload.source)
        assertEquals(1_700_000_000L, payload.iat)
    }

    @Test
    fun send_gone_isRejectedAndKeepsSession() = test { controller ->
        sender.result = Result.failure(ServerException.Request(HttpStatusCode.Gone, "Gone"))

        controller.onSend()

        assertEquals(PairSendErrorKind.Rejected, controller.state.await<PairSendViewState.Error>().kind)
        assertEquals(0, gateway.clearCalls)
        assertEquals(0, transferred)
    }

    @Test
    fun send_badRequest_isRejected() = test { controller ->
        sender.result = Result.failure(ServerException.Request(HttpStatusCode.BadRequest, "Bad"))

        controller.onSend()

        assertEquals(PairSendErrorKind.Rejected, controller.state.await<PairSendViewState.Error>().kind)
        assertEquals(0, gateway.clearCalls)
    }

    @Test
    fun send_connectionFailure_isNetwork() = test { controller ->
        sender.result = Result.failure(PairingSenderException.NotDelivered(java.net.ConnectException("refused")))

        controller.onSend()

        assertEquals(PairSendErrorKind.Network, controller.state.await<PairSendViewState.Error>().kind)
        assertEquals(0, gateway.clearCalls)
    }

    @Test
    fun send_otherStatus_isUnknown() = test { controller ->
        sender.result = Result.failure(ServerException.Request(HttpStatusCode.ServiceUnavailable, "Busy"))

        controller.onSend()

        assertEquals(PairSendErrorKind.Unknown, controller.state.await<PairSendViewState.Error>().kind)
        assertEquals(0, gateway.clearCalls)
    }

    @Test
    fun send_notLoggedIn_doesNotSend() = test { controller ->
        gateway.check = PairSendSessionCheck.NotLoggedIn

        controller.onSend()

        assertEquals(PairSendErrorKind.NotLoggedIn, controller.state.await<PairSendViewState.Error>().kind)
        assertTrue(sender.sent.isEmpty())
        assertEquals(0, gateway.exclusiveCalls)
    }

    @Test
    fun send_tokensMissingUnderLock_isNotLoggedIn() = test { controller ->
        gateway.exclusiveSession = session.copy(refreshToken = null)

        controller.onSend()

        assertEquals(PairSendErrorKind.NotLoggedIn, controller.state.await<PairSendViewState.Error>().kind)
        assertTrue(sender.sent.isEmpty())
    }

    @Test
    fun send_sealsSessionReadUnderLock() = test { controller ->
        gateway.exclusiveSession = session.copy(accessToken = "fresh-access", refreshToken = "fresh-refresh")

        controller.onSend()

        controller.state.await<PairSendViewState.Done>()
        val payload = Json.decodeFromString<PairingPayload>(
            PairingEnvelope.open(target.key, target.sid, sender.sent.single().second).decodeToString()
        )
        assertEquals("fresh-access", payload.accessToken)
        assertEquals("fresh-refresh", payload.refreshToken)
    }

    @Test
    fun send_notFound_isRejected() = test { controller ->
        sender.result = Result.failure(ServerException.Request(HttpStatusCode.NotFound, "Not found"))

        controller.onSend()

        assertEquals(PairSendErrorKind.Rejected, controller.state.await<PairSendViewState.Error>().kind)
        assertEquals(0, gateway.clearCalls)
    }

    @Test
    fun send_invalidTarget_isRejected() = test { controller ->
        sender.result = Result.failure(PairingSenderException.InvalidTarget())

        controller.onSend()

        assertEquals(PairSendErrorKind.Rejected, controller.state.await<PairSendViewState.Error>().kind)
    }

    @Test
    fun send_uncertainDelivery_isUncertainAndKeepsSession() = test { controller ->
        sender.result = Result.failure(PairingSenderException.Uncertain(java.io.IOException("reset")))

        controller.onSend()

        assertEquals(PairSendErrorKind.Uncertain, controller.state.await<PairSendViewState.Error>().kind)
        assertEquals(0, gateway.clearCalls)
    }

    @Test
    fun send_profileCheckNetworkFailure_doesNotSend() = test { controller ->
        gateway.check = PairSendSessionCheck.Network

        controller.onSend()

        assertEquals(PairSendErrorKind.Network, controller.state.await<PairSendViewState.Error>().kind)
        assertTrue(sender.sent.isEmpty())
    }

    @Test
    fun retry_afterFailure_sendsAgain() = test { controller ->
        sender.result = Result.failure(PairingSenderException.NotDelivered(java.net.ConnectException("refused")))
        controller.onSend()
        controller.state.await<PairSendViewState.Error>()

        sender.result = Result.success(Unit)
        controller.onRetry()

        controller.state.await<PairSendViewState.Done>()
        assertEquals(2, sender.sent.size)
        assertEquals(1, gateway.clearCalls)
    }

    private fun test(block: suspend CoroutineScope.(PairSendController) -> Unit) = runBlocking {
        val scope = CoroutineScope(coroutineContext[ContinuationInterceptor]!! + SupervisorJob())
        val controller = PairSendController(
            scope = scope,
            target = target,
            gateway = gateway,
            senderProvider = { sender },
            nowSeconds = { 1_700_000_000L },
            onTransferred = { transferred++ },
        )
        try {
            block(controller)
        } finally {
            scope.cancel()
        }
    }

    private inner class FakeGateway : PairSendSessionGateway {
        var check = PairSendSessionCheck.Valid
        var exclusiveSession = session
        var clearCalls = 0
        var clearedInsideExclusive = false
        var exclusiveCalls = 0
        private var insideExclusive = false

        override suspend fun checkSession(): PairSendSessionCheck = check

        override suspend fun <T> withExclusiveSession(block: suspend (AuthSession) -> T): T {
            exclusiveCalls++
            insideExclusive = true
            try {
                return block(exclusiveSession)
            } finally {
                insideExclusive = false
            }
        }

        override suspend fun clearLocalSession() {
            clearCalls++
            clearedInsideExclusive = insideExclusive
        }
    }

    private class FakeSender : PairingSender {
        var result: Result<Unit> = Result.success(Unit)
        val sent = mutableListOf<Pair<PairingTarget, String>>()

        override suspend fun send(target: PairingTarget, sealed: String): Result<Unit> {
            sent += target to sealed
            return result
        }
    }
}
