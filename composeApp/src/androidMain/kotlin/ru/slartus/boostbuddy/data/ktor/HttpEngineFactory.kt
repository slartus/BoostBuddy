package ru.slartus.boostbuddy.data.ktor

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager


internal actual class HttpEngineFactory actual constructor() {
    actual fun createEngine(isDebug: Boolean): HttpClientEngineFactory<HttpClientEngineConfig> {
        return if (isDebug) UnsafeOkHttp else OkHttp
    }
}

internal object UnsafeOkHttp : HttpClientEngineFactory<OkHttpConfig> {
    private fun OkHttpClient.Builder.trustAllCertifiesIfNeeded() {
        hostnameVerifier { _, _ -> true }
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), SecureRandom())
        sslSocketFactory(sslContext.socketFactory, trustManager)
    }

    override fun create(block: OkHttpConfig.() -> Unit): HttpClientEngine =
        OkHttp.create {
            config {
                trustAllCertifiesIfNeeded()
            }
        }
}