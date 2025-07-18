package ru.slartus.boostbuddy.components.filter

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.components.filter.tags.FilterTagsComponent
import ru.slartus.boostbuddy.components.filter.tags.FilterTagsComponentImpl

@Stable
interface FilterComponent {
    val viewStates: Value<FilterViewState>
    val dialogSlot: Value<ChildSlot<*, DialogChild>>

    fun onAccessTypeChange(accessType: AccessType)
    fun onTagsClick()
    fun onDialogDismissed()
    fun onPeriodClick()
    fun onDateRangeReset()
    fun onDateRangeSelected(from: Clock, to: Clock)

    sealed class DialogChild {
        object Period : DialogChild()
        class Tags(val component: FilterTagsComponent) : DialogChild()
    }
}

internal class FilterComponentImpl(
    componentContext: ComponentContext,
    private val params: FilterParams
) : BaseComponent<FilterViewState, FilterViewAction>(
    componentContext,
    FilterViewState(filter = params.filter)
), FilterComponent {

    private val dialogNavigation = SlotNavigation<DialogConfig>()

    private val filterTagsComponent = FilterTagsComponentImpl(
        componentContext = componentContext,
        entryPoint = params.entryPoint,
        selectedTags = params.filter.tags,
        onTagsChange = ::onTagsChange
    )

    override val dialogSlot: Value<ChildSlot<*, FilterComponent.DialogChild>> = childSlot(
        key = "dialogSlot",
        source = dialogNavigation,
        serializer = DialogConfig.serializer(),
        handleBackButton = true,
        childFactory = ::createDialogChild
    )

    override fun onAccessTypeChange(accessType: AccessType) {
        updateFilter { copy(accessType = accessType) }
    }

    override fun onTagsClick() {
        dialogNavigation.activate(DialogConfig.Tags)
    }

    override fun onDialogDismissed() {
        dialogNavigation.dismiss()
    }

    override fun onPeriodClick() {
        dialogNavigation.activate(DialogConfig.Period)
    }

    override fun onDateRangeReset() {
        updateFilter { copy(period = null) }
    }

    override fun onDateRangeSelected(from: Clock, to: Clock) {
        updateFilter { copy(period = Period(from.startOfDay(), to.endOfDay())) }
    }

    private fun createDialogChild(
        config: DialogConfig,
        componentContext: ComponentContext
    ): FilterComponent.DialogChild = when (config) {
        DialogConfig.Period -> FilterComponent.DialogChild.Period
        DialogConfig.Tags -> FilterComponent.DialogChild.Tags(filterTagsComponent)
    }

    private fun onTagsChange(tags: List<Tag>) {
        updateFilter { copy(tags = tags) }
    }

    private fun updateFilter(transform: Filter.() -> Filter) {
        viewState = viewState.copy(
            filter = viewState.filter.transform()
        )
        params.onFilter(viewState.filter)
    }

    @Serializable
    private sealed interface DialogConfig {
        @Serializable
        data object Period : DialogConfig

        @Serializable
        data object Tags : DialogConfig
    }

    private companion object{
        fun Clock.startOfDay(): Clock {
            val today = this.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val startOfDay = LocalDateTime(today.year, today.monthNumber, today.dayOfMonth, 0, 0)
                .toInstant(TimeZone.currentSystemDefault())
            return object : Clock {
                override fun now(): Instant = startOfDay
            }
        }

        fun Clock.endOfDay(): Clock {
            val today = this.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val endOfDay = LocalDateTime(today.year, today.monthNumber, today.dayOfMonth, 23, 59, 59, 999_999_999)
                .toInstant(TimeZone.currentSystemDefault())
            return object : Clock {
                override fun now(): Instant = endOfDay
            }
        }
    }
}