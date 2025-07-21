package ru.slartus.boostbuddy.data.analytic

import io.github.aakira.napier.Napier

class LogAnalytics : AnalyticsTracker {
    override fun trackEvent(event: AnalyticsEvent) {
        Napier.i(event.toString())
    }

    override fun reportUnhandledException(exception: Throwable) =
        Napier.e("UnhandledException", exception)

    override fun d(message: String) = Unit

    override fun d(message: () -> String) = Unit

    override fun i(message: String) = Unit

    override fun i(message: () -> String) = Unit

    override fun w(message: String) = Unit

    override fun w(message: () -> String) = Unit

    override fun w(throwable: Throwable, message: String) = Unit

    override fun w(throwable: Throwable, message: () -> String) = Unit

    override fun e(message: String) = Unit

    override fun e(message: () -> String) = Unit

    override fun e(throwable: Throwable, message: String) = Unit

    override fun e(throwable: Throwable, message: () -> String) = Unit
}