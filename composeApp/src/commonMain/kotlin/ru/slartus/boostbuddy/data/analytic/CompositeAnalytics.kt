package ru.slartus.boostbuddy.data.analytic

class CompositeAnalytics(private val analytics: List<AnalyticsTracker>) : AnalyticsTracker {
    override fun trackEvent(event: AnalyticsEvent) {
        analytics.forEach { it.trackEvent(event) }
    }
}