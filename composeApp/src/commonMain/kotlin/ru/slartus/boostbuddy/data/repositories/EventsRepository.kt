package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.call.body
import ru.slartus.boostbuddy.data.api.BoostyApi
import ru.slartus.boostbuddy.data.repositories.models.Event
import ru.slartus.boostbuddy.data.repositories.models.EventType
import ru.slartus.boostbuddy.data.repositories.models.EventsResponse
import ru.slartus.boostbuddy.utils.dateTimeFromUnix
import ru.slartus.boostbuddy.utils.fetchOrError

internal class EventsRepository(
    private val boostyApi: BoostyApi,
) {
    suspend fun getEvents(): Result<List<Event>> =
        fetchOrError {
            val response: EventsResponse = boostyApi.events().body()
            response.toEventsList()
        }
}

private fun EventsResponse.toEventsList(): List<Event> =
    data?.notificationStandalone?.events?.mapNotNull { it.toEventOrNull() }.orEmpty()

private fun EventsResponse.Event.toEventOrNull(): Event? {
    return Event(
        isRead = isRead == true,
        blogUrl = blog?.blogUrl ?: return null,
        type = type?.toEventType() ?: return null,
        eventTime = eventTime?.let { dateTimeFromUnix(it) } ?: return null,
        id = id ?: return null
    )
}

private fun String.toEventType(): EventType = when (this) {
    "video_stream_start" -> EventType.VideoStreamStart
    else -> EventType.Other
}