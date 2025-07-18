package ru.slartus.boostbuddy.components.filter

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Clock

@Immutable
data class FilterViewState(
    val filter: Filter = Filter()
) {
    val tagsText: String = if (filter.tags.isEmpty()) {
        "Теги"
    } else {
        "Теги: ${filter.tags.joinToString { it.title }}"
    }
}

@Immutable
data class Filter(
    val accessType: AccessType = AccessType.All,
    val period: Period? = null,
    val tags: List<Tag> = emptyList()
)

data class Period(val from: Clock, val to: Clock)

data class Tag(val id: String, val title: String)

enum class AccessType {
    All,
    Allowed,
    Bought
}