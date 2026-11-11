package ru.slartus.boostbuddy.data.pairing

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.content.TextContent
import io.ktor.http.path
import kotlinx.coroutines.CancellationException
import ru.slartus.boostbuddy.data.log.logger
import ru.slartus.boostbuddy.utils.toServerException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.nio.channels.UnresolvedAddressException

internal class CioPairingSender(
    private val allowLoopback: Boolean,
    private val timeoutMillis: Long = TIMEOUT_MILLIS,
) : PairingSender {

    override suspend fun send(target: PairingTarget, sealed: String): Result<Unit> {
        if (!isPrivateIpv4(target.host, allowLoopback)) {
            return Result.failure(PairingSenderException.InvalidTarget())
        }
        return try {
            createHttpClient().use { httpClient ->
                httpClient.post {
                    url {
                        protocol = URLProtocol.HTTP
                        host = target.host
                        port = target.port
                        path("s", target.sid)
                    }
                    setBody(TextContent(sealed, ContentType.Text.Plain))
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ResponseException) {
            Result.failure(e.toServerException())
        } catch (e: Exception) {
            logger.e(e, "Pairing send failed")
            Result.failure(
                if (e.isConnectFailure()) {
                    PairingSenderException.NotDelivered(e)
                } else {
                    PairingSenderException.Uncertain(e)
                }
            )
        }
    }

    private fun Throwable.isConnectFailure(): Boolean =
        generateSequence(this) { it.cause }.any {
            it is ConnectTimeoutException ||
                    it is ConnectException ||
                    it is NoRouteToHostException ||
                    it is UnresolvedAddressException
        }

    private fun createHttpClient(): HttpClient =
        HttpClient(CIO) {
            expectSuccess = true
            followRedirects = false
            install(HttpTimeout) {
                connectTimeoutMillis = timeoutMillis
                requestTimeoutMillis = timeoutMillis
                socketTimeoutMillis = timeoutMillis
            }
        }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
