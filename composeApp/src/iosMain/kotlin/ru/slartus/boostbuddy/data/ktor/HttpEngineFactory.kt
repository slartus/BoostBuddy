package ru.slartus.boostbuddy.data.ktor

import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*

internal actual class HttpEngineFactory actual constructor() {
    actual fun createEngine(isDebug: Boolean): HttpClientEngineFactory<HttpClientEngineConfig> = Darwin
}