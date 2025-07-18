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

    sealed class DialogChild {
        class Period(val from: Clock = Clock.System, val to: Clock = Clock.System) : DialogChild()
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

    private fun createDialogChild(
        config: DialogConfig,
        componentContext: ComponentContext
    ): FilterComponent.DialogChild = when (config) {
        DialogConfig.Period -> FilterComponent.DialogChild.Period()
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
}