package ru.slartus.boostbuddy.data.ktor

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import org.kodein.di.bindSingleton
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.log.CompositeLogger
import ru.slartus.boostbuddy.data.log.Logger
import ru.slartus.boostbuddy.data.repositories.AuthSession
import ru.slartus.boostbuddy.data.repositories.InMemorySettings
import ru.slartus.boostbuddy.data.repositories.RecordingObservableSettings
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.models.TokenRefreshResponse
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoostyTokenRefresherTest {

    private val now = 1_800_000_000L
    private val settings = SettingsRepository(
        settings = RecordingObservableSettings(InMemorySettings()),
        authSettings = RecordingObservableSettings(InMemorySettings()),
        coroutineScope = CoroutineScope(Job()),
    )
    private val refreshCalls = mutableListOf<Pair<String, String>>()
    private var refreshOutcome: TokenRefreshOutcome = success("new-access", "new-refresh", 3600)
    private var refreshGate: CompletableDeferred<Unit>? = null
    private val refresher = BoostyTokenRefresher(
        settingsRepository = settings,
        refreshRequest = { refreshToken, deviceId ->
            refreshCalls += refreshToken to deviceId
            refreshGate?.await()
            refreshOutcome
        },
        nowSeconds = { now },
    )

    private var server: EmbeddedServer<*, *>? = null

    @BeforeTest
    fun setUp() {
        Inject.createDependenciesTree { bindSingleton<Logger> { CompositeLogger(emptyList()) } }
    }

    @AfterTest
    fun tearDown() {
        server?.stop(0, 0)
    }

    @Test
    fun ensureValidToken_refreshesWhenExpiringSoon() = runBlocking<Unit> {
        store(expiresAt = now + 60)

        assertEquals("new-access", refresher.ensureValidToken())

        assertEquals(listOf("old-refresh" to "device"), refreshCalls)
        assertEquals(AuthSession("new-access", "new-refresh", now + 3600, "device"), settings.getSession())
    }

    @Test
    fun ensureValidToken_keepsTokenWhenFarFromExpiry() = runBlocking<Unit> {
        store(expiresAt = now + 3600)

        assertEquals("old-access", refresher.ensureValidToken())

        assertTrue(refreshCalls.isEmpty())
    }

    @Test
    fun pendingSession_blocksProactiveAndForcedRefresh() = runBlocking<Unit> {
        settings.beginPendingSession(AuthSession("donor-access", "donor-refresh", now + 10, "donor-device"))

        assertEquals("donor-access", refresher.ensureValidToken())
        assertNull(refresher.forceRefresh())

        assertTrue(refreshCalls.isEmpty())
    }

    @Test
    fun commitPendingSession_refreshesWithDonorDeviceAndClearsPending() = runBlocking<Unit> {
        settings.beginPendingSession(AuthSession("donor-access", "donor-refresh", now + 10, "donor-device"))

        val outcome = refresher.commitPendingSession()

        assertTrue(outcome is TokenRefreshOutcome.Success)
        assertEquals(listOf("donor-refresh" to "donor-device"), refreshCalls)
        assertEquals(AuthSession("new-access", "new-refresh", now + 3600, "donor-device"), settings.getSession())
        assertFalse(settings.hasPendingSession())
    }

    @Test
    fun commitPendingSession_failureKeepsPending() = runBlocking<Unit> {
        val donor = AuthSession("donor-access", "donor-refresh", now + 10, "donor-device")
        settings.beginPendingSession(donor)
        refreshOutcome = TokenRefreshOutcome.NetworkError

        assertEquals(TokenRefreshOutcome.NetworkError, refresher.commitPendingSession())

        assertTrue(settings.hasPendingSession())
        assertEquals(donor, settings.getSession())
    }

    @Test
    fun commitPendingSession_withoutPendingIsRejected() = runBlocking<Unit> {
        store(expiresAt = now + 3600)

        assertEquals(TokenRefreshOutcome.Rejected, refresher.commitPendingSession())
        assertTrue(refreshCalls.isEmpty())
    }

    @Test
    fun withExclusiveSession_refreshesWhenShortLivedAndPassesFreshSession() = runBlocking<Unit> {
        store(expiresAt = now + 10 * 60)

        val session = refresher.withExclusiveSession { it }

        assertEquals(1, refreshCalls.size)
        assertEquals("new-access", session.accessToken)
        assertEquals("new-refresh", session.refreshToken)
    }

    @Test
    fun withExclusiveSession_blocksConcurrentRefresh() = runBlocking<Unit> {
        store(expiresAt = now + 3600)
        val release = CompletableDeferred<Unit>()
        val entered = CompletableDeferred<Unit>()

        val exclusive = async {
            refresher.withExclusiveSession {
                entered.complete(Unit)
                release.await()
                settings.getRefreshToken()
            }
        }
        entered.await()
        val forced = async { refresher.forceRefresh() }
        repeat(10) { yield() }

        assertTrue(refreshCalls.isEmpty())
        release.complete(Unit)

        assertEquals("old-refresh", exclusive.await())
        assertEquals("new-access", forced.await())
        assertEquals(1, refreshCalls.size)
    }

    @Test
    fun plugin_retriesWithRefreshedTokenAfterUnauthorized() = runBlocking<Unit> {
        store(expiresAt = now + 3600)
        val client = client()

        val body = client.get(url()).bodyAsText()

        assertEquals("ok", body)
        assertEquals(1, refreshCalls.size)
        assertEquals<List<String?>>(listOf("Bearer old-access", "Bearer new-access"), seenAuthorization)
    }

    @Test
    fun plugin_skipTokenRefreshDoesNotRefreshOnUnauthorized() = runBlocking<Unit> {
        store(expiresAt = now + 60)
        val client = client()

        val result = runCatching { client.get(url()) { attributes.put(SkipTokenRefresh, Unit) }.body<String>() }

        assertTrue(result.isFailure)
        assertTrue(refreshCalls.isEmpty())
        assertEquals<List<String?>>(listOf("Bearer old-access"), seenAuthorization)
    }

    @Test
    fun plugin_refreshesProactivelyBeforeRequest() = runBlocking<Unit> {
        store(expiresAt = now + 60)
        val client = client()

        assertEquals("ok", client.get(url()).bodyAsText())

        assertEquals(1, refreshCalls.size)
        assertEquals<List<String?>>(listOf("Bearer new-access"), seenAuthorization)
    }

    private val seenAuthorization = mutableListOf<String?>()

    private fun client() = buildBoostyHttpClient(
        debugLog = false,
        settingsRepository = settings,
        tokenRefresher = refresher,
        json = Json,
    )

    private fun url(): String {
        val started = embeddedServer(CIO, port = 0, host = "127.0.0.1") {
            routing {
                get("/check") {
                    val authorization = call.request.headers[HttpHeaders.Authorization]
                    seenAuthorization += authorization
                    if (authorization == "Bearer new-access") {
                        call.respondText("ok")
                    } else {
                        call.respond(HttpStatusCode.Unauthorized)
                    }
                }
            }
        }.start(wait = false)
        server = started
        val port = runBlocking { started.engine.resolvedConnectors().first().port }
        return "http://127.0.0.1:$port/check"
    }

    private suspend fun store(expiresAt: Long) {
        settings.replaceSession("old-access", "old-refresh", expiresAt, "device")
    }

    private fun success(access: String, refresh: String, expiresIn: Long) =
        TokenRefreshOutcome.Success(TokenRefreshResponse(access, refresh, expiresIn))
}
