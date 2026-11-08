package ru.slartus.boostbuddy.data.pairing

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import org.kodein.di.bindSingleton
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.log.CompositeLogger
import ru.slartus.boostbuddy.data.log.Logger
import ru.slartus.boostbuddy.utils.ServerException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CioPairingSenderTest {

    private val received = mutableListOf<Triple<String?, String, String>>()
    private var responseStatus = HttpStatusCode.OK
    private val server: EmbeddedServer<*, *> = embeddedServer(CIO, port = 0, host = "127.0.0.1") {
        routing {
            post("/s/{sid}") {
                received += Triple(call.parameters["sid"], call.request.contentType().toString(), call.receiveText())
                call.respond(responseStatus)
            }
        }
    }.start(wait = false)

    private val port = runBlocking { server.engine.resolvedConnectors().first().port }
    private val sid = PairingEnvelope.newSid()

    @BeforeTest
    fun setUp() {
        Inject.createDependenciesTree { bindSingleton<Logger> { CompositeLogger(emptyList()) } }
    }

    @AfterTest
    fun tearDown() {
        server.stop(0, 0)
    }

    @Test
    fun send_postsSealedTextToSidPath() = runBlocking {
        val result = CioPairingSender(allowLoopback = true).send(target("127.0.0.1"), "sealed-value")

        assertTrue(result.isSuccess)
        assertEquals(listOf<Triple<String?, String, String>>(Triple(sid, "text/plain", "sealed-value")), received)
    }

    @Test
    fun send_failsOnErrorStatus() = runBlocking {
        responseStatus = HttpStatusCode.Gone

        val error = CioPairingSender(allowLoopback = true).send(target("127.0.0.1"), "sealed").exceptionOrNull()

        assertIs<ServerException.Request>(error)
        assertEquals(HttpStatusCode.Gone, error.code)
    }

    @Test
    fun send_rejectsLoopbackWhenNotAllowed() = runBlocking {
        val result = CioPairingSender(allowLoopback = false).send(target("127.0.0.1"), "sealed")

        assertTrue(result.isFailure)
        assertTrue(received.isEmpty())
    }

    @Test
    fun send_rejectsPublicHost() = runBlocking {
        val result = CioPairingSender(allowLoopback = true).send(target("8.8.8.8"), "sealed")

        assertTrue(result.isFailure)
    }

    private fun target(host: String) =
        PairingTarget(host = host, port = port, sid = sid, key = PairingEnvelope.newKey())
}
