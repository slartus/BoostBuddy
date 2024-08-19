package ru.slartus.boostbuddy.components.events

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.slartus.boostbuddy.components.common.ProgressState
import ru.slartus.boostbuddy.data.repositories.models.Event

data class EventsViewState(
    val items: ImmutableList<Event> = persistentListOf(),
    val progressState: ProgressState = ProgressState.Init
)

