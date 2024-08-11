package ru.slartus.boostbuddy.data.repositories.models

data class Event(
    val isRead: Boolean,
    val blog: String,
    val type: EventType,
    val eventTime: Int,
    val id: Int,
)

enum class EventType {
    VideoStreamStart, Other
}