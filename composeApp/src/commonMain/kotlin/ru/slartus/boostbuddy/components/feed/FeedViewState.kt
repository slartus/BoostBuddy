package ru.slartus.boostbuddy.components.feed

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.slartus.boostbuddy.components.common.ProgressState
import ru.slartus.boostbuddy.components.filter.Filter
import ru.slartus.boostbuddy.data.repositories.models.Extra

data class FeedViewState(
    val items: ImmutableList<FeedPostItem> = persistentListOf(),
    val extra: Extra? = null,
    val progressState: ProgressState = ProgressState.Init,
    val filter: Filter = Filter(),
) {
    val hasMore: Boolean = extra?.isLast == false
}