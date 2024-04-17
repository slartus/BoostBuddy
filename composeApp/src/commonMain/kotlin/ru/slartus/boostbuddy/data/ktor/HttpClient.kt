package ru.slartus.boostbuddy.data.ktor

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.slartus.boostbuddy.data.repositories.SettingsRepository

internal const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

internal fun buildHttpClient(isDebug: Boolean, settingsRepository: SettingsRepository?) =
    HttpClient(HttpEngineFactory().createEngine(isDebug)) {

        expectSuccess = true

        install(HttpTimeout) {
            connectTimeoutMillis = 15000
            requestTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }

        install(UserAgent) {
            agent = USER_AGENT
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Napier.d(message)
                }
            }
            level = if (isDebug) LogLevel.ALL else LogLevel.NONE
        }

        if (settingsRepository != null) {
            install(Auth) {
                bearer {
                    loadTokens {
                        BearerTokens(settingsRepository.getAccessToken().orEmpty(), "")
                    }
                }
            }
        }
    }