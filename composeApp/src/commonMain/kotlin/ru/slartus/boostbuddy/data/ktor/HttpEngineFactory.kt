package ru.slartus.boostbuddy.data.ktor

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory

internal expect class HttpEngineFactory constructor() {
    fun createEngine(isDebug: Boolean): HttpClientEngineFactory<HttpClientEngineConfig>
}