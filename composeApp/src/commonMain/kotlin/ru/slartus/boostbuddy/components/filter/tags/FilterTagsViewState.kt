package ru.slartus.boostbuddy.components.filter.tags

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.slartus.boostbuddy.components.filter.Tag
import ru.slartus.boostbuddy.data.repositories.models.Extra

data class FilterTagsViewState(
    val tags: ImmutableList<TagItem> = persistentListOf(),
    val extra: Extra? = null,
)

sealed class TagItem {
    data class TagModel(val tag: Tag, val selected: Boolean) : TagItem()
    data object Loading : TagItem()
    data object Error : TagItem()
}