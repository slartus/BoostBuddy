package ru.slartus.boostbuddy.components.filter

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Clock

@Immutable
data class FilterViewState(
    val onlyAllowed: Boolean = false,
    val period: Period? = null,
    val tags: List<Tag> = emptyList()
) {
}

data class Period(val from: Clock, val to: Clock)

data class Tag(val id: Int, val name: String)