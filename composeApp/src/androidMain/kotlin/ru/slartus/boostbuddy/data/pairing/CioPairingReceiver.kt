package ru.slartus.boostbuddy.data.pairing

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.readByteArray
import ru.slartus.boostbuddy.data.pairing.web.PairingWebPages
import java.io.IOException
import java.net.BindException
import java.net.InetAddress
import java.net.NetworkInterface

internal class CioPairingReceiver(
    private val allowLoopback: Boolean,
    private val lanAddress: () -> String?,
    private val ports: IntRange = DEFAULT_PORTS,
    private val wallClock: () -> Long = System::currentTimeMillis,
    private val monotonicClock: () -> Long = { System.nanoTime() / 1_000_000 },
    private val ttlMillis: Long = TTL_MILLIS,
    private val lingerMillis: Long = LINGER_MILLIS,
    private val bodyReadTimeoutMillis: Long = BODY_READ_TIMEOUT_MILLIS,
) : PairingReceiver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val startMutex = Mutex()
    private val _events = MutableSharedFlow<PairingReceiverEvent>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val stateLock = Any()
    private var generation = 0
    private var current: ActiveSession? = null

    override val events: Flow<PairingReceiverEvent> = _events.asSharedFlow()

    override suspend fun start(): Result<PairingSession> {
        val startGeneration = synchronized(stateLock) { generation }
        return startMutex.withLock {
            try {
                Result.success(withContext(Dispatchers.IO) { startLocked(startGeneration) })
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                Result.failure(e)
            }
        }
    }

    override fun stop() {
        val session = synchronized(stateLock) {
            generation++
            current
        }
        session?.close()
        _events.resetReplayCache()
    }

    private suspend fun startLocked(startGeneration: Int): PairingSession {
        val previous = synchronized(stateLock) { current.also { current = null } }
        previous?.let {
            it.close()
            it.shutdown().join()
        }
        _events.resetReplayCache()
        val host = lanAddress()?.takeIf { isPrivateIpv4(it, allowLoopback) }
            ?: throw PairingReceiverException.NoLan()
        if (!isLocalAddress(host)) throw PairingReceiverException.NoLan()
        var bindError: Throwable? = null
        for (port in ports) {
            val session = ActiveSession(
                host = host,
                port = port,
                deadlineMonotonicMs = monotonicClock() + ttlMillis,
                expiresAtMs = wallClock() + ttlMillis,
            )
            try {
                session.server.startSuspend(wait = false)
            } catch (e: Exception) {
                session.close()
                currentCoroutineContext().ensureActive()
                if (!e.isBindFailure()) throw PairingReceiverException.StartFailed(e)
                bindError = e
                continue
            }
            session.scheduleExpiry()
            val published = synchronized(stateLock) {
                (generation == startGeneration).also { if (it) current = session }
            }
            if (!published) {
                session.close()
                throw PairingReceiverException.Stopped()
            }
            return session.publicSession
        }
        throw PairingReceiverException.NoFreePort(bindError)
    }

    private inner class ActiveSession(
        host: String,
        port: Int,
        private val deadlineMonotonicMs: Long,
        expiresAtMs: Long,
    ) {
        private val lock = Any()
        private val key = PairingEnvelope.newKey()
        private val sid = PairingEnvelope.newSid()
        private val authority = "$host:$port"
        private val requestPermits = Semaphore(MAX_CONCURRENT_REQUESTS)
        private var failures = 0
        private var finished = false

        @Volatile
        private var expiryJob: Job? = null

        val publicSession = PairingSession(
            pageUrl = PairingLink.receiverPageUrl(PairingTarget(host = host, port = port, sid = sid, key = key)),
            host = host,
            port = port,
            sid = sid,
            expiresAtMs = expiresAtMs,
        )

        val server: EmbeddedServer<*, *> = embeddedServer(
            factory = CIO,
            configure = {
                connector {
                    this.host = host
                    this.port = port
                }
                connectionIdleTimeoutSeconds = CONNECTION_IDLE_TIMEOUT_SECONDS
                reuseAddress = true
            },
            module = { pairingModule() },
        )

        private val shutdownJob = scope.launch(start = CoroutineStart.LAZY) {
            expiryJob?.cancel()
            withContext(NonCancellable) {
                server.stopSuspend(SHUTDOWN_GRACE_MILLIS, SHUTDOWN_TIMEOUT_MILLIS)
            }
        }

        fun scheduleExpiry() {
            expiryJob = scope.launch {
                delay(ttlMillis)
                synchronized(lock) { finishWith(PairingReceiverEvent.Expired(sid)) }
            }
        }

        fun close() {
            synchronized(lock) { invalidate() }
            shutdown()
        }

        fun shutdown(): Job {
            shutdownJob.start()
            return shutdownJob
        }

        private fun Application.pairingModule() {
            intercept(ApplicationCallPipeline.Plugins) {
                SECURITY_HEADERS.forEach { (name, value) -> call.response.headers.append(name, value) }
                if (call.request.headers[HttpHeaders.Host] != authority) {
                    call.respond(MISDIRECTED_REQUEST)
                    finish()
                    return@intercept
                }
                if (!requestPermits.tryAcquire()) {
                    call.respond(HttpStatusCode.ServiceUnavailable)
                    finish()
                    return@intercept
                }
                try {
                    proceed()
                } finally {
                    requestPermits.release()
                }
            }
            routing {
                PairingWebPages.assets.forEach { (path, asset) ->
                    val contentType = ContentType.parse(asset.contentType)
                    get(path) { call.respondText(asset.body, contentType) }
                }
                post("${PairingWebPages.PATH_SEND_PREFIX}{sid}") {
                    val status = receive(call)
                    if (status == HttpStatusCode.RequestTimeout || status == HttpStatusCode.PayloadTooLarge) {
                        call.response.headers.append(HttpHeaders.Connection, "close")
                    }
                    call.respond(status)
                }
            }
        }

        private suspend fun receive(call: ApplicationCall): HttpStatusCode {
            if (!isAccepting()) return HttpStatusCode.Gone
            if (call.parameters["sid"] != sid) return HttpStatusCode.NotFound
            if (!call.request.contentType().match(ContentType.Text.Plain)) {
                return fail(HttpStatusCode.UnsupportedMediaType)
            }
            val declaredLength = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
            if (declaredLength != null && declaredLength > MAX_BODY_BYTES) {
                return fail(HttpStatusCode.PayloadTooLarge)
            }
            val body = withTimeoutOrNull(bodyReadTimeoutMillis) {
                call.receiveChannel().readRemaining(MAX_BODY_BYTES + 1L).readByteArray()
            } ?: return HttpStatusCode.RequestTimeout
            if (body.size > MAX_BODY_BYTES) return fail(HttpStatusCode.PayloadTooLarge)
            return accept(body.decodeToString().trim())
        }

        private fun accept(sealed: String): HttpStatusCode = synchronized(lock) {
            if (!isAcceptingLocked()) return HttpStatusCode.Gone
            val payload = PairingPayloadValidator.openOrNull(key = key, sid = sid, sealed = sealed)
                ?: return failLocked(HttpStatusCode.BadRequest)
            finishWith(PairingReceiverEvent.Received(sid, payload))
            HttpStatusCode.OK
        }

        private fun isAccepting(): Boolean = synchronized(lock) { isAcceptingLocked() }

        private fun isAcceptingLocked(): Boolean {
            if (finished) return false
            if (monotonicClock() >= deadlineMonotonicMs) {
                finishWith(PairingReceiverEvent.Expired(sid))
                return false
            }
            return true
        }

        private fun fail(status: HttpStatusCode): HttpStatusCode = synchronized(lock) { failLocked(status) }

        private fun failLocked(status: HttpStatusCode): HttpStatusCode {
            if (finished) return HttpStatusCode.Gone
            failures++
            if (failures >= MAX_FAILURES) finishWith(PairingReceiverEvent.TooManyAttempts(sid))
            return status
        }

        private fun finishWith(event: PairingReceiverEvent) {
            if (!invalidate()) return
            _events.tryEmit(event)
            scope.launch {
                delay(lingerMillis)
                shutdown()
            }
        }

        private fun invalidate(): Boolean {
            if (finished) return false
            finished = true
            key.fill(0)
            return true
        }
    }

    private fun isLocalAddress(host: String): Boolean =
        try {
            NetworkInterface.getByInetAddress(InetAddress.getByName(host)) != null
        } catch (e: IOException) {
            false
        }

    private fun Throwable.isBindFailure(): Boolean =
        generateSequence(this) { it.cause }.any { it is BindException }

    internal companion object {
        val DEFAULT_PORTS = 8765..8770
        private val MISDIRECTED_REQUEST = HttpStatusCode(421, "Misdirected Request")
        const val TTL_MILLIS = 5 * 60 * 1000L
        const val LINGER_MILLIS = 3_000L
        const val BODY_READ_TIMEOUT_MILLIS = 10_000L
        const val MAX_BODY_BYTES = PairingWebPages.MAX_SEALED_BYTES
        const val MAX_FAILURES = 10
        const val MAX_CONCURRENT_REQUESTS = 8
        private const val CONNECTION_IDLE_TIMEOUT_SECONDS = 10
        private const val SHUTDOWN_GRACE_MILLIS = 200L
        private const val SHUTDOWN_TIMEOUT_MILLIS = 1_000L

        val SECURITY_HEADERS = listOf(
            "Content-Security-Policy" to "default-src 'none'; script-src 'self'; style-src 'self'; " +
                    "connect-src 'self'; img-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'",
            HttpHeaders.CacheControl to "no-store",
            "Referrer-Policy" to "no-referrer",
            "X-Content-Type-Options" to "nosniff",
        )
    }
}
