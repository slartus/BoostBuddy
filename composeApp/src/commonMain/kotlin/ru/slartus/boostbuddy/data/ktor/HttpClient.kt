package ru.slartus.boostbuddy.data.ktor

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import ru.slartus.boostbuddy.data.log.logger
import ru.slartus.boostbuddy.data.repositories.AuthSession
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.models.TokenRefreshResponse

internal const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

internal const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36"

private const val REFRESH_THRESHOLD_SECONDS = 300L // 5 минут до истечения

internal val SkipTokenRefresh = AttributeKey<Unit>("BoostySkipTokenRefresh")

internal sealed class TokenRefreshOutcome {
    data class Success(val response: TokenRefreshResponse) : TokenRefreshOutcome() {
        override fun toString(): String = "Success"
    }

    data object Rejected : TokenRefreshOutcome()
    data object NetworkError : TokenRefreshOutcome()
}

internal class BoostyRefreshApi(clientFactory: () -> HttpClient) {
    private val client by lazy(clientFactory)

    suspend fun refresh(refreshToken: String, deviceId: String): TokenRefreshOutcome =
        try {
            val response: TokenRefreshResponse = client.post("oauth/token/") {
                header("X-From-Id", deviceId)
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("device_id", deviceId)
                            append("device_os", "web")
                            append("grant_type", "refresh_token")
                            append("refresh_token", refreshToken)
                        }
                    )
                )
            }.body()
            if (response.accessToken.isNullOrEmpty()) {
                logger.d("Token refresh returned empty access_token")
                TokenRefreshOutcome.Rejected
            } else {
                TokenRefreshOutcome.Success(response)
            }
        } catch (e: ClientRequestException) {
            logger.e(e, "Token refresh rejected")
            TokenRefreshOutcome.Rejected
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e, "Token refresh failed")
            TokenRefreshOutcome.NetworkError
        }
}

internal sealed class ExclusiveSessionResult<out T> {
    data class Completed<T>(val value: T) : ExclusiveSessionResult<T>()
    data object SessionPending : ExclusiveSessionResult<Nothing>()
    data object SessionRejected : ExclusiveSessionResult<Nothing>()
}

internal class BoostyTokenRefresher(
    private val settingsRepository: SettingsRepository,
    private val refreshRequest: suspend (refreshToken: String, deviceId: String) -> TokenRefreshOutcome,
    private val nowSeconds: () -> Long = { Clock.System.now().epochSeconds },
) {
    private val refreshMutex = Mutex()
    private val pendingSession = MutableStateFlow<Boolean?>(null)

    suspend fun ensureValidToken(): String? = refreshMutex.withLock {
        val accessToken = settingsRepository.getAccessToken()
        if (accessToken.isNullOrEmpty()) return@withLock null
        if (isPending()) return@withLock accessToken

        val expiresAt = settingsRepository.getTokenExpiresAt()
        if (expiresAt != null) {
            val now = nowSeconds()
            if (expiresAt - now < REFRESH_THRESHOLD_SECONDS) {
                logger.d("Token expires soon (in ${expiresAt - now}s), proactive refresh")
                return@withLock doRefresh() ?: accessToken
            }
        }

        accessToken
    }

    suspend fun forceRefresh(): String? = refreshMutex.withLock {
        if (isPending()) return@withLock null
        doRefresh()
    }

    suspend fun beginPendingSession(session: AuthSession) = refreshMutex.withLock {
        settingsRepository.beginPendingSession(session)
        pendingSession.value = true
    }

    suspend fun rollbackPendingSession() = refreshMutex.withLock {
        settingsRepository.rollbackPendingSessionIfAny()
        pendingSession.value = false
    }

    suspend fun commitPendingSession(): TokenRefreshOutcome = refreshMutex.withLock {
        if (!isPending()) return@withLock TokenRefreshOutcome.Rejected
        val session = settingsRepository.getSession()
        val refreshToken = session.refreshToken
        if (refreshToken.isNullOrEmpty()) return@withLock TokenRefreshOutcome.Rejected
        val deviceId = session.deviceId ?: settingsRepository.getOrCreateDeviceId()
        withContext(NonCancellable) {
            settingsRepository.confirmPendingSession()
            val outcome = refreshRequest(refreshToken, deviceId)
            if (outcome is TokenRefreshOutcome.Success) {
                settingsRepository.commitPendingSession(outcome.response.applyTo(session.copy(deviceId = deviceId)))
                pendingSession.value = false
                logger.d("Pending session committed")
            }
            outcome
        }
    }

    suspend fun <T> withExclusiveSession(block: suspend (AuthSession) -> T): ExclusiveSessionResult<T> =
        refreshMutex.withLock {
            if (isPending()) return@withLock ExclusiveSessionResult.SessionPending
            val expiresAt = settingsRepository.getTokenExpiresAt()
            if (expiresAt != null && expiresAt - nowSeconds() < EXCLUSIVE_SESSION_MIN_TTL_SECONDS) {
                if (refreshStoredSession() == TokenRefreshOutcome.Rejected) {
                    return@withLock ExclusiveSessionResult.SessionRejected
                }
            }
            ExclusiveSessionResult.Completed(block(settingsRepository.getSession()))
        }

    private suspend fun isPending(): Boolean =
        pendingSession.value ?: settingsRepository.hasPendingSession().also { pendingSession.value = it }

    private suspend fun doRefresh(): String? =
        (refreshStoredSession() as? TokenRefreshOutcome.Success)?.response?.accessToken

    private suspend fun refreshStoredSession(): TokenRefreshOutcome {
        val storedRefreshToken = settingsRepository.getRefreshToken()
        if (storedRefreshToken.isNullOrEmpty()) return TokenRefreshOutcome.Rejected

        val deviceId = settingsRepository.getOrCreateDeviceId()
        return withContext(NonCancellable) {
            val outcome = refreshRequest(storedRefreshToken, deviceId)
            val newAccessToken = (outcome as? TokenRefreshOutcome.Success)?.response?.accessToken
                ?: return@withContext outcome
            val response = outcome.response
            settingsRepository.putAccessToken(newAccessToken)
            response.refreshToken?.let { settingsRepository.putRefreshToken(it) }

            val expiresIn = response.expiresIn
            if (expiresIn != null && expiresIn > 0) {
                settingsRepository.putTokenExpiresAt(nowSeconds() + expiresIn)
            }

            logger.d("Token refreshed successfully")
            outcome
        }
    }

    private fun TokenRefreshResponse.applyTo(session: AuthSession): AuthSession = session.copy(
        accessToken = accessToken,
        refreshToken = refreshToken ?: session.refreshToken,
        expiresAt = expiresIn?.takeIf { it > 0 }?.let { nowSeconds() + it } ?: session.expiresAt,
    )

    private companion object {
        const val EXCLUSIVE_SESSION_MIN_TTL_SECONDS = 15 * 60L
    }
}

internal fun buildBoostyRefreshHttpClient(debugLog: Boolean, json: Json): HttpClient =
    HttpClient(HttpEngineFactory().createEngine(debugLog)) {
        expectSuccess = true
        install(ContentNegotiation) { json(json) }
        install(UserAgent) { agent = DESKTOP_USER_AGENT }
        install(HttpTimeout) {
            connectTimeoutMillis = 15000
            requestTimeoutMillis = 30000
        }
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.boosty.to"
            }
            header("X-App", "web")
            header("X-Locale", "ru_RU")
            header(HttpHeaders.Referrer, "https://boosty.to/")
            header(HttpHeaders.Origin, "https://boosty.to")
            header(HttpHeaders.Accept, "application/json, text/plain, */*")
            headers.remove(HttpHeaders.AcceptCharset)
        }
    }

internal fun buildBoostyHttpClient(
    debugLog: Boolean,
    settingsRepository: SettingsRepository,
    tokenRefresher: BoostyTokenRefresher,
    json: Json
): HttpClient {
    val tokenRefreshPlugin = createClientPlugin("BoostyTokenRefresh") {
        on(Send) { request ->
            val skipRefresh = request.attributes.contains(SkipTokenRefresh)
            val token = if (skipRefresh) {
                settingsRepository.getAccessToken()
            } else {
                tokenRefresher.ensureValidToken()
            }
            if (!token.isNullOrEmpty()) {
                request.headers[HttpHeaders.Authorization] = "Bearer $token"
            }

            val originalCall = proceed(request)

            if (!skipRefresh && originalCall.response.status == HttpStatusCode.Unauthorized) {
                val newToken = tokenRefresher.forceRefresh()
                if (newToken != null) {
                    request.headers[HttpHeaders.Authorization] = "Bearer $newToken"
                    proceed(request)
                } else {
                    originalCall
                }
            } else {
                originalCall
            }
        }
    }

    return buildHttpClient(debugLog, json) {
        install(tokenRefreshPlugin)
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.boosty.to"
            }
        }
    }
}

/**
 * Клиент для phone-auth эндпоинтов Boosty: без Auth-плагина (чтобы не уходил пустой
 * `Authorization: Bearer`), с десктопным User-Agent и web-специфичными заголовками
 * (`X-App`, `X-Locale`, `Referer`, `Origin`, `Accept`), как шлёт браузер.
 * `X-From-Id` выставляется на уровне каждого запроса отдельно (это deviceId).
 */
internal fun buildBoostyAuthHttpClient(debugLog: Boolean, json: Json) =
    buildHttpClient(debugLog, json, userAgent = DESKTOP_USER_AGENT) {
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.boosty.to"
            }
            header("X-App", "web")
            header("X-Locale", "ru_RU")
            header(HttpHeaders.Referrer, "https://boosty.to/")
            header(HttpHeaders.Origin, "https://boosty.to")
            header(HttpHeaders.Accept, "application/json, text/plain, */*")
            headers.remove(HttpHeaders.AcceptCharset)
        }
    }

internal fun buildGithubHttpClient(json: Json, debugLog: Boolean) =
    buildHttpClient(debugLog, json) {
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.github.com"
            }
        }
    }

internal fun buildHttpClient(
    isDebug: Boolean,
    json: Json,
    userAgent: String = USER_AGENT,
    block: HttpClientConfig<HttpClientEngineConfig>.() -> Unit = {}
): HttpClient =
    HttpClient(HttpEngineFactory().createEngine(isDebug)) {

        expectSuccess = true

        install(HttpTimeout) {
            connectTimeoutMillis = 15000
            requestTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }

        install(UserAgent) {
            agent = userAgent
        }

        install(ContentNegotiation) {
            json(json)
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    ru.slartus.boostbuddy.data.log.logger.d(maskSecrets(message))
                }
            }
            level = if (isDebug) LogLevel.ALL else LogLevel.NONE
            sanitizeHeader { header ->
                listOf(HttpHeaders.Authorization, HttpHeaders.Cookie, HttpHeaders.SetCookie)
                    .any { it.equals(header, ignoreCase = true) }
            }
        }
        block(this)
    }

private val secretLogPatterns = listOf(
    Regex("Bearer\\s+\\S+", RegexOption.IGNORE_CASE) to "Bearer ***",
    Regex("((?:refresh_token|access_token)=)[^&\\s]+") to "\$1***",
    Regex("(\"(?:accessToken|refreshToken|access_token|refresh_token)\"\\s*:\\s*)\"[^\"]*\"") to "\$1\"***\"",
)

private fun maskSecrets(message: String): String =
    secretLogPatterns.fold(message) { acc, (regex, replacement) -> regex.replace(acc, replacement) }
