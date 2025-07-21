package ru.slartus.boostbuddy.data.analytic

class CompositeAnalytics(private val analytics: List<AnalyticsTracker>) : AnalyticsTracker {
    override fun trackEvent(event: AnalyticsEvent) =
        analytics.forEach { it.trackEvent(event) }

    override fun reportUnhandledException(exception: Throwable) =
        analytics.forEach { it.reportUnhandledException(exception) }

    override fun d(message: String) =
        analytics.forEach { it.d(message) }

    override fun d(message: () -> String) =
        analytics.forEach { it.d(message) }

    override fun i(message: String) =
        analytics.forEach { it.i(message) }

    override fun i(message: () -> String) =
        analytics.forEach { it.i(message) }

    override fun w(message: String) =
        analytics.forEach { it.w(message) }

    override fun w(message: () -> String) =
        analytics.forEach { it.w(message) }

    override fun w(throwable: Throwable, message: String) =
        analytics.forEach { it.w(throwable, message) }

    override fun w(throwable: Throwable, message: () -> String) =
        analytics.forEach { it.w(throwable, message) }

    override fun e(message: String) =
        analytics.forEach { it.e(message) }

    override fun e(message: () -> String) =
        analytics.forEach { it.e(message) }

    override fun e(throwable: Throwable, message: String) =
        analytics.forEach { it.e(throwable, message) }

    override fun e(throwable: Throwable, message: () -> String) =
        analytics.forEach { it.e(throwable, message) }
}