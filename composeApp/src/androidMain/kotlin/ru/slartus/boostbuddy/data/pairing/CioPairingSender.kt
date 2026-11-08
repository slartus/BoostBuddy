package ru.slartus.boostbuddy.data.pairing

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.content.TextContent
import io.ktor.http.path
import ru.slartus.boostbuddy.utils.fetchOrError

internal class CioPairingSender(
    private val allowLoopback: Boolean,
) : PairingSender {

    override suspend fun send(target: PairingTarget, sealed: String): Result<Unit> = fetchOrError {
        require(isPrivateIpv4(target.host, allowLoopback)) { "Pairing target is not a private LAN address" }
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
        Unit
    }

    private fun createHttpClient(): HttpClient =
        HttpClient(CIO) {
            expectSuccess = true
            followRedirects = false
            install(HttpTimeout) {
                connectTimeoutMillis = TIMEOUT_MILLIS
                requestTimeoutMillis = TIMEOUT_MILLIS
                socketTimeoutMillis = TIMEOUT_MILLIS
            }
        }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
