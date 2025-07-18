package ru.slartus.boostbuddy.components.filter

data class FilterParams(
    val filter: Filter,
    val entryPoint: FilterScreenEntryPoint,
    val onFilter: (Filter) -> Unit
)

sealed class FilterScreenEntryPoint {
    data object Feed : FilterScreenEntryPoint()
    class Blog(val blog: ru.slartus.boostbuddy.data.repositories.Blog) : FilterScreenEntryPoint()
}