package ru.slartus.boostbuddy.data.analytic

import ru.slartus.boostbuddy.data.log.logger

class LogAnalytics : AnalyticsTracker {
    override fun trackEvent(event: AnalyticsEvent) {
        logger.i(event.toString())
    }
}