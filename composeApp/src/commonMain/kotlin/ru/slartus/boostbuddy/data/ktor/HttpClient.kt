package ru.slartus.boostbuddy.data.ktor

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout

fun buildHttpClient(
    isDebug: Boolean,

    ) = HttpClient(HttpEngineFactory().createEngine(isDebug)) {

    expectSuccess = true


    install(HttpTimeout) {
        connectTimeoutMillis = 15000
        requestTimeoutMillis = 30000
        socketTimeoutMillis = 30000
    }

}