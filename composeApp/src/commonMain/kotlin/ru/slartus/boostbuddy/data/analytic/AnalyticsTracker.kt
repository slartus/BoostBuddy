package ru.slartus.boostbuddy.data.analytic

import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.log.Logger

val analytics: AnalyticsTracker
    get() = Inject.instance()

interface AnalyticsTracker : Logger {
    fun trackEvent(event: AnalyticsEvent)

    fun trackEvent(name: String, params: Map<String, Any> = emptyMap()) =
        trackEvent(BaseAnalyticsEvent(name, params))

    fun reportUnhandledException(exception: Throwable)
}

interface AnalyticsEvent {
    val name: String
    var params: Map<String, Any>
}

class BaseAnalyticsEvent(
    override val name: String,
    override var params: Map<String, Any> = emptyMap()
) : AnalyticsEvent {
    override fun toString(): String {
        return "AnalyticsEvent(name='$name', params=$params)"
    }
}