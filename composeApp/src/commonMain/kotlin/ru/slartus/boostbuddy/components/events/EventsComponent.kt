package ru.slartus.boostbuddy.components.events

import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.components.common.ProgressState
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.EventsRepository
import ru.slartus.boostbuddy.utils.messageOrThrow

interface EventsComponent {
}

internal class EventsComponentImpl(
    componentContext: ComponentContext
) : BaseComponent<EventsViewState, Any>(
    componentContext,
    EventsViewState()
), EventsComponent {
    private val eventsRepository by Inject.lazy<EventsRepository>()

    init {
        fetchEvents()
    }

    private fun fetchEvents() {
        scope.launch {
            viewState = viewState.copy(
                progressState = ProgressState.Loading
            )
            val result = eventsRepository.getEvents()
            if (result.isSuccess) {
                val events = result.getOrThrow()
                viewState = viewState.copy(
                    items = events.toPersistentList(),
                    progressState = ProgressState.Loaded
                )
            } else {
                viewState = viewState.copy(
                    progressState = ProgressState.Error(
                        result.exceptionOrNull()?.messageOrThrow() ?: "Ошибка загрузки"
                    )
                )
            }
        }
    }
}