package ru.slartus.boostbuddy.data.repositories.models

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char

@Immutable
data class Event(
    val isRead: Boolean,
    val blogUrl: String,
    val type: EventType,
    val eventTime: LocalDateTime,
    val id: Int,
) {
    val eventTimeText: String = eventTime.format(format)

    companion object {
        val format = LocalDateTime.Format {
            year()
            char('-')
            monthNumber()
            char('-')
            dayOfMonth()
            char(' ')
            hour()
            char(':')
            minute()
            char(':')
            second()
        }
    }
}

enum class EventType {
    VideoStreamStart, Other
}