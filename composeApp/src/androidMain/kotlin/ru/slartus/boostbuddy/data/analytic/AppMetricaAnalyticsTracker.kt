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

    override fun trackEvent(event: AnalyticsEvent) {
        AppMetrica.reportEvent(event.name, event.params)
    }

    private companion object {
        const val API_KEY = "8ce08a04-8395-4c50-bae3-7d0d74d8c63c"
    }
}