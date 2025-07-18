package ru.slartus.boostbuddy.components.filter

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Immutable
data class FilterViewState(
    val filter: Filter = Filter()
) {
    val dateRangeText: String = when {
        filter.period == null -> "За всё время"
        filter.period.to == filter.period.from -> filter.period.to.getCurrentDateFormatted()
        else -> "${filter.period.from.getCurrentDateFormatted()} - ${filter.period.to.getCurrentDateFormatted()}"
    }

    val tagsText: String = if (filter.tags.isEmpty()) {
        "Теги"
    } else {
        "Теги: ${filter.tags.joinToString { it.title }}"
    }

    private companion object {
        fun Clock.getCurrentDateFormatted(): String {
            val now = this.now()
            val date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
            val day = date.dayOfMonth.toString().padStart(2, '0')
            val month = date.monthNumber.toString().padStart(2, '0')
            val year = date.year.toString().takeLast(2)

            return "$day.$month.$year"
        }
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