package ru.slartus.boostbuddy.components.feed

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.slartus.boostbuddy.components.common.ProgressState

data class FeedViewState(
    val items: ImmutableList<FeedPostItem> = persistentListOf(),
    val hasMore: Boolean = true,
    val progressState: ProgressState = ProgressState.Init,
)