package ru.slartus.boostbuddy.data.analytic

import android.content.Context
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig

class AppMetricaAnalyticsTracker(context: Context) : AnalyticsTracker {
    init {
        val config = AppMetricaConfig.newConfigBuilder(API_KEY)
            .build()
        AppMetrica.activate(context, config)
    }

    override fun trackEvent(event: AnalyticsEvent) =
        AppMetrica.reportEvent(event.name, event.params)

    override fun reportUnhandledException(exception: Throwable) =
        AppMetrica.reportUnhandledException(exception)

    override fun d(message: String) =
        AppMetrica.reportEvent("debug", mapOf("message" to message))

    override fun d(message: () -> String) =
        AppMetrica.reportEvent("debug", mapOf("message" to message()))

    override fun i(message: String) =
        AppMetrica.reportEvent("info", mapOf("message" to message))

    override fun i(message: () -> String) =
        AppMetrica.reportEvent("info", mapOf("message" to message()))

    override fun w(message: String) =
        AppMetrica.reportEvent("warn", mapOf("message" to message))

    override fun w(message: () -> String) =
        AppMetrica.reportEvent("warn", mapOf("message" to message()))

    override fun w(throwable: Throwable, message: String) =
        AppMetrica.reportError("warn", message, throwable)

    override fun w(throwable: Throwable, message: () -> String) =
        AppMetrica.reportError("warn", message(), throwable)

    override fun e(message: String) =
        AppMetrica.reportError("error", message, null)

    override fun e(message: () -> String) =
        AppMetrica.reportError("error", message(), null)

    override fun e(throwable: Throwable, message: String) =
        AppMetrica.reportError("error", message, throwable)

    override fun e(throwable: Throwable, message: () -> String) =
        AppMetrica.reportError("error", message(), throwable)

    private companion object {
        const val API_KEY = "8ce08a04-8395-4c50-bae3-7d0d74d8c63c"
    }
}