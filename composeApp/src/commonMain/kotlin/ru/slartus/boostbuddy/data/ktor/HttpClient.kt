package ru.slartus.boostbuddy.data.ktor

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngineConfig
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import ru.slartus.boostbuddy.data.log.logger
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.models.TokenRefreshResponse

internal const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

internal const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36"

private const val REFRESH_THRESHOLD_SECONDS = 300L // 5 минут до истечения

internal fun buildBoostyHttpClient(
    debugLog: Boolean,
    settingsRepository: SettingsRepository,
    json: Json
): HttpClient {
    val refreshMutex = Mutex()

    val refreshClient by lazy {
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
    }

    suspend fun doRefresh(): String? {
        val storedRefreshToken = settingsRepository.getRefreshToken()
        if (storedRefreshToken.isNullOrEmpty()) return null

        return try {
            val deviceId = settingsRepository.getOrCreateDeviceId()
            val response: TokenRefreshResponse = refreshClient.post("oauth/token/") {
                header("X-From-Id", deviceId)
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("device_id", deviceId)
                            append("device_os", "web")
                            append("grant_type", "refresh_token")
                            append("refresh_token", storedRefreshToken)
                        }
                    )
                )
            }.body()

            val newAccessToken = response.accessToken
            if (newAccessToken.isNullOrEmpty()) {
                logger.d("Token refresh returned empty access_token")
                return null
            }

            settingsRepository.putAccessToken(newAccessToken)
            response.refreshToken?.let { settingsRepository.putRefreshToken(it) }

            val expiresIn = response.expiresIn
            if (expiresIn != null && expiresIn > 0) {
                val expiresAt = Clock.System.now().epochSeconds + expiresIn
                settingsRepository.putTokenExpiresAt(expiresAt)
            }

            logger.d("Token refreshed successfully")
            newAccessToken
        } catch (e: Exception) {
            logger.e(e, "Token refresh failed")
            null
        }
    }

    suspend fun ensureValidToken(): String? = refreshMutex.withLock {
        val accessToken = settingsRepository.getAccessToken()
        if (accessToken.isNullOrEmpty()) return@withLock null

        val expiresAt = settingsRepository.getTokenExpiresAt()
        if (expiresAt != null) {
            val now = Clock.System.now().epochSeconds
            if (expiresAt - now < REFRESH_THRESHOLD_SECONDS) {
                logger.d("Token expires soon (in ${expiresAt - now}s), proactive refresh")
                return@withLock doRefresh() ?: accessToken
            }
        }

        accessToken
    }

    suspend fun forceRefresh(): String? = refreshMutex.withLock {
        doRefresh()
    }

    val tokenRefreshPlugin = createClientPlugin("BoostyTokenRefresh") {
        on(Send) { request ->
            val token = ensureValidToken()
            if (!token.isNullOrEmpty()) {
                request.headers[HttpHeaders.Authorization] = "Bearer $token"
            }

            val originalCall = proceed(request)

            if (originalCall.response.status == HttpStatusCode.Unauthorized) {
                val newToken = forceRefresh()
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
                    ru.slartus.boostbuddy.data.log.logger.d(
                        message.replace(
                            Regex("Bearer\\s+[^\\s-]+", RegexOption.IGNORE_CASE),
                            "Bearer secret"
                        )
                    )
                }
            }
            level = if (isDebug) LogLevel.ALL else LogLevel.NONE
        }
        block(this)
    }
