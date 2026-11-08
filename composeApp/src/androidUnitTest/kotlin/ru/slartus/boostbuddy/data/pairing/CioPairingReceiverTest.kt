package ru.slartus.boostbuddy.data.pairing

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.kodein.di.bindSingleton
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.log.CompositeLogger
import ru.slartus.boostbuddy.data.log.Logger
import ru.slartus.boostbuddy.data.pairing.web.PairingWebPages
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class CioPairingReceiverTest {

    private var now = 1_700_000_000_000L
    private var monotonicNow = 5_000L
    private val firstPort = freePort()
    private val receivers = mutableListOf<CioPairingReceiver>()
    private val blockers = mutableListOf<ServerSocket>()

    @BeforeTest
    fun setUp() {
        Inject.createDependenciesTree { bindSingleton<Logger> { CompositeLogger(emptyList()) } }
    }

    @AfterTest
    fun tearDown() {
        receivers.forEach { it.stop() }
        blockers.forEach { it.close() }
    }

    @Test
    fun getPage_servesHtmlWithSecurityHeaders() = runBlocking {
        val session = startedSession()

        val response = request(session, "GET", PairingWebPages.PATH_PAGE_P)

        assertEquals(200, response.status)
        assertEquals("text/html; charset=utf-8", response.header("Content-Type"))
        assertEquals(EXPECTED_CSP, response.header("Content-Security-Policy"))
        assertEquals("no-store", response.header("Cache-Control"))
        assertEquals("no-referrer", response.header("Referrer-Policy"))
        assertEquals("nosniff", response.header("X-Content-Type-Options"))
        assertNull(response.header("Access-Control-Allow-Origin"))
        assertTrue(response.body.contains("<script src=\"/p.js\"></script>"))
    }

    @Test
    fun getAssets_serveEveryAssetWithItsContentType() = runBlocking {
        val session = startedSession()

        PairingWebPages.assets.forEach { (path, asset) ->
            val response = request(session, "GET", path)

            assertEquals(200, response.status, path)
            assertEquals(asset.contentType, response.header("Content-Type"), path)
            assertEquals("no-store", response.header("Cache-Control"), path)
        }
    }

    @Test
    fun request_withForeignHost_isRejected() = runBlocking {
        val session = startedSession()

        val page = request(session, "GET", PairingWebPages.PATH_PAGE_P, host = "evil.example:${session.port}")
        val post = request(session, "POST", sendPath(session), host = "localhost:${session.port}", body = seal(session))

        assertEquals(421, page.status)
        assertEquals(421, post.status)
        assertEquals("no-store", page.header("Cache-Control"))
        assertEquals(200, request(session, "POST", sendPath(session), body = seal(session)).status)
    }

    @Test
    fun post_withDeclaredOversizedBody_returns413() = runBlocking {
        val session = startedSession()

        val response = request(session, "POST", sendPath(session), body = ByteArray(9000) { 'A'.code.toByte() })

        assertEquals(413, response.status)
    }

    @Test
    fun post_withChunkedOversizedBody_returns413() = runBlocking {
        val session = startedSession()

        val response = request(session, "POST", sendPath(session), body = ByteArray(9000) { 'A'.code.toByte() }, chunked = true)

        assertEquals(413, response.status)
    }

    @Test
    fun post_withUnknownSid_returns404() = runBlocking {
        val session = startedSession()

        val response = request(session, "POST", PairingWebPages.PATH_SEND_PREFIX + PairingEnvelope.newSid(), body = seal(session))

        assertEquals(404, response.status)
    }

    @Test
    fun post_withNonTextContentType_returns415() = runBlocking {
        val session = startedSession()

        val response = request(session, "POST", sendPath(session), body = seal(session), contentType = "application/json")

        assertEquals(415, response.status)
    }

    @Test
    fun post_withGarbage_returns400AndKeepsSessionOpen() = runBlocking {
        val session = startedSession()

        val garbage = request(session, "POST", sendPath(session), body = "garbage".encodeToByteArray())
        val foreignKey = request(
            session,
            "POST",
            sendPath(session),
            body = PairingEnvelope.seal(PairingEnvelope.newKey(), session.sid, payloadJson().encodeToByteArray()).encodeToByteArray(),
        )

        assertEquals(400, garbage.status)
        assertEquals(400, foreignKey.status)
        assertEquals(200, request(session, "POST", sendPath(session), body = seal(session)).status)
    }

    @Test
    fun post_withSkewedSenderClock_isAccepted() = runBlocking {
        val twoDaysSeconds = 2 * 24 * 60 * 60L
        val behind = startedSession()
        val ahead = startedSession()

        assertEquals(200, request(behind, "POST", sendPath(behind), body = seal(behind, payloadJson(iat = now / 1000 - twoDaysSeconds))).status)
        assertEquals(200, request(ahead, "POST", sendPath(ahead), body = seal(ahead, payloadJson(iat = now / 1000 + twoDaysSeconds))).status)
    }

    @Test
    fun post_withInvalidPayload_returns400() = runBlocking {
        val session = startedSession()

        assertEquals(400, request(session, "POST", sendPath(session), body = seal(session, payloadJson(deviceId = " "))).status)
        assertEquals(400, request(session, "POST", sendPath(session), body = seal(session, "{}")).status)
    }

    @Test
    fun post_withValidEnvelope_returns200AndEmitsNormalizedPayload() = runBlocking {
        val receiver = receiver()
        val session = receiver.start().getOrThrow()

        val response = request(session, "POST", sendPath(session), body = seal(session, payloadJson(expiresAt = 1_800_000_000_123L)))

        assertEquals(200, response.status)
        val event = assertIs<PairingReceiverEvent.Received>(nextEvent(receiver))
        assertEquals(session.sid, event.sid)
        assertEquals("access", event.payload.accessToken)
        assertEquals("refresh", event.payload.refreshToken)
        assertEquals("device", event.payload.deviceId)
        assertEquals(1_800_000_000L, event.payload.expiresAt)
    }

    @Test
    fun post_afterSuccess_returns410() = runBlocking {
        val session = startedSession()
        val sealed = seal(session)

        assertEquals(200, request(session, "POST", sendPath(session), body = sealed).status)
        assertEquals(410, request(session, "POST", sendPath(session), body = sealed).status)
    }

    @Test
    fun post_afterTtl_returns410AndEmitsExpired() = runBlocking {
        val receiver = receiver()
        val session = receiver.start().getOrThrow()
        val sealed = seal(session)
        assertEquals(now + CioPairingReceiver.TTL_MILLIS, session.expiresAtMs)
        monotonicNow += CioPairingReceiver.TTL_MILLIS

        val response = request(session, "POST", sendPath(session), body = sealed)

        assertEquals(410, response.status)
        assertEquals(PairingReceiverEvent.Expired(session.sid), nextEvent(receiver))
    }

    @Test
    fun post_afterWallClockJump_isStillAccepted() = runBlocking {
        val session = startedSession()
        val sealed = seal(session)
        now += CioPairingReceiver.TTL_MILLIS * 10

        assertEquals(200, request(session, "POST", sendPath(session), body = sealed).status)
    }

    @Test
    fun post_afterTenFailures_returns410AndEmitsTooManyAttempts() = runBlocking {
        val receiver = receiver()
        val session = receiver.start().getOrThrow()

        repeat(CioPairingReceiver.MAX_FAILURES) {
            assertEquals(400, request(session, "POST", sendPath(session), body = "bad".encodeToByteArray()).status)
        }

        assertEquals(410, request(session, "POST", sendPath(session), body = seal(session)).status)
        assertEquals(PairingReceiverEvent.TooManyAttempts(session.sid), nextEvent(receiver))
    }

    @Test
    fun server_stopsItselfAfterSuccess() = runBlocking {
        val session = receiver(lingerMillis = 0).start().getOrThrow()

        assertEquals(200, request(session, "POST", sendPath(session), body = seal(session)).status)

        assertPortReleased(session.port)
    }

    @Test
    fun stop_releasesPortAndRestartIssuesNewSecrets() = runBlocking {
        val receiver = receiver(ports = firstPort..firstPort)
        val first = receiver.start().getOrThrow()

        receiver.stop()
        receiver.stop()
        assertPortReleased(first.port)
        val second = receiver.start().getOrThrow()

        assertEquals(first.port, second.port)
        assertNotEquals(first.sid, second.sid)
        assertNotEquals(PairingLink.parse(first.pageUrl, allowLoopback = true)?.key?.toList(), PairingLink.parse(second.pageUrl, allowLoopback = true)?.key?.toList())
        assertEquals(404, request(second, "POST", sendPath(first), body = seal(second)).status)
    }

    @Test
    fun stop_duringStart_cancelsStartAndReleasesPort() = runBlocking {
        lateinit var receiver: CioPairingReceiver
        receiver = receiver(
            ports = firstPort..firstPort,
            lanAddress = {
                receiver.stop()
                LOOPBACK
            },
        )

        val error = receiver.start().exceptionOrNull()

        assertIs<PairingReceiverException.Stopped>(error)
        assertPortReleased(firstPort)
    }

    @Test
    fun stop_dropsReceivedEventForNewSubscribers() = runBlocking {
        val receiver = receiver()
        val session = receiver.start().getOrThrow()
        assertEquals(200, request(session, "POST", sendPath(session), body = seal(session)).status)

        receiver.stop()

        assertNull(withTimeoutOrNull(300) { receiver.events.first() })
    }

    @Test
    fun start_failsWhenAddressIsNotLocal() = runBlocking<Unit> {
        val error = receiver(lanAddress = { "10.255.255.1" }).start().exceptionOrNull()

        assertIs<PairingReceiverException.StartFailed>(error)
    }

    @Test
    fun start_skipsBusyPort() = runBlocking {
        blockers += ServerSocket(firstPort, 1, InetAddress.getByName(LOOPBACK))

        val session = receiver(ports = firstPort..firstPort + 1).start().getOrThrow()

        assertEquals(firstPort + 1, session.port)
    }

    @Test
    fun start_failsWhenAllPortsBusy() = runBlocking<Unit> {
        blockers += ServerSocket(firstPort, 1, InetAddress.getByName(LOOPBACK))

        val error = receiver(ports = firstPort..firstPort).start().exceptionOrNull()

        assertIs<PairingReceiverException.NoFreePort>(error)
    }

    @Test
    fun start_failsWithoutLanAddress() = runBlocking<Unit> {
        val missing = receiver(lanAddress = { null }).start().exceptionOrNull()
        val loopbackInRelease = receiver(allowLoopback = false).start().exceptionOrNull()
        val public = receiver(lanAddress = { "8.8.8.8" }).start().exceptionOrNull()

        assertIs<PairingReceiverException.NoLan>(missing)
        assertIs<PairingReceiverException.NoLan>(loopbackInRelease)
        assertIs<PairingReceiverException.NoLan>(public)
    }

    @Test
    fun session_pageUrlPointsToBoundServer() = runBlocking {
        val session = startedSession()

        val target = assertNotNull(PairingLink.parse(session.pageUrl, allowLoopback = true))

        assertEquals(LOOPBACK, target.host)
        assertEquals(session.port, target.port)
        assertEquals(session.sid, target.sid)
        assertFalse(session.toString().contains("#"))
    }

    @Test
    fun cioSender_deliversPayloadToReceiver() = runBlocking {
        val receiver = receiver()
        val session = receiver.start().getOrThrow()
        val target = assertNotNull(PairingLink.parse(session.pageUrl, allowLoopback = true))
        val sealed = PairingEnvelope.seal(
            target.key,
            target.sid,
            payloadJson(iat = System.currentTimeMillis() / 1000, source = PairingPayload.SOURCE_APP).encodeToByteArray(),
        )

        val result = CioPairingSender(allowLoopback = true).send(target, sealed)

        assertTrue(result.isSuccess)
        val event = assertIs<PairingReceiverEvent.Received>(nextEvent(receiver))
        assertEquals(PairingPayload.SOURCE_APP, event.payload.source)
    }

    private suspend fun startedSession(): PairingSession = receiver().start().getOrThrow()

    private fun receiver(
        allowLoopback: Boolean = true,
        lanAddress: () -> String? = { LOOPBACK },
        ports: IntRange = firstPort..firstPort + 3,
        monotonicClock: () -> Long = { monotonicNow },
        lingerMillis: Long = 60_000L,
    ): CioPairingReceiver =
        CioPairingReceiver(
            allowLoopback = allowLoopback,
            lanAddress = lanAddress,
            ports = ports,
            wallClock = { now },
            monotonicClock = monotonicClock,
            lingerMillis = lingerMillis,
        ).also { receivers += it }

    private suspend fun nextEvent(receiver: CioPairingReceiver): PairingReceiverEvent =
        withTimeout(5_000) { receiver.events.first() }

    private fun sendPath(session: PairingSession): String = PairingWebPages.PATH_SEND_PREFIX + session.sid

    private fun seal(session: PairingSession, json: String = payloadJson()): ByteArray {
        val target = checkNotNull(PairingLink.parse(session.pageUrl, allowLoopback = true))
        return PairingEnvelope.seal(target.key, target.sid, json.encodeToByteArray()).encodeToByteArray()
    }

    private fun payloadJson(
        iat: Long = now / 1000,
        expiresAt: Long? = null,
        deviceId: String = "device",
        source: String = PairingPayload.SOURCE_WEB,
    ): String = Json.encodeToString(
        PairingPayload(
            accessToken = "access",
            refreshToken = "refresh",
            expiresAt = expiresAt,
            deviceId = deviceId,
            source = source,
            iat = iat,
        )
    )

    private fun assertPortReleased(port: Int) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            try {
                Socket(LOOPBACK, port).close()
            } catch (e: ConnectException) {
                return
            }
            Thread.sleep(50)
        }
        fail("Port $port is still accepting connections")
    }

    private fun request(
        session: PairingSession,
        method: String,
        path: String,
        host: String = "${session.host}:${session.port}",
        body: ByteArray? = null,
        chunked: Boolean = false,
        contentType: String = "text/plain",
    ): RawResponse = Socket(LOOPBACK, session.port).use { socket ->
        socket.soTimeout = 5_000
        val head = buildString {
            append("$method $path HTTP/1.1\r\nHost: $host\r\nConnection: close\r\n")
            if (body != null) {
                append("Content-Type: $contentType\r\n")
                if (chunked) append("Transfer-Encoding: chunked\r\n") else append("Content-Length: ${body.size}\r\n")
            }
            append("\r\n")
        }
        val output = socket.getOutputStream()
        try {
            output.write(head.encodeToByteArray())
            if (body != null) {
                if (chunked) output.write("${body.size.toString(16)}\r\n".encodeToByteArray())
                output.write(body)
                if (chunked) output.write("\r\n0\r\n\r\n".encodeToByteArray())
            }
            output.flush()
        } catch (e: IOException) {
            Unit
        }
        RawResponse.parse(socket.getInputStream().readBytes().decodeToString())
    }

    private class RawResponse(val status: Int, private val headers: Map<String, String>, val body: String) {
        fun header(name: String): String? = headers[name.lowercase()]

        companion object {
            fun parse(raw: String): RawResponse {
                val head = raw.substringBefore("\r\n\r\n")
                val lines = head.split("\r\n")
                val status = lines.first().split(' ')[1].toInt()
                val headers = lines.drop(1).associate {
                    it.substringBefore(':').trim().lowercase() to it.substringAfter(':').trim()
                }
                return RawResponse(status, headers, raw.substringAfter("\r\n\r\n"))
            }
        }
    }

    private companion object {
        const val LOOPBACK = "127.0.0.1"
        const val EXPECTED_CSP = "default-src 'none'; script-src 'self'; style-src 'self'; connect-src 'self'; " +
                "img-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'"

        fun freePort(): Int = ServerSocket(0, 1, InetAddress.getByName(LOOPBACK)).use { it.localPort }
    }
}
