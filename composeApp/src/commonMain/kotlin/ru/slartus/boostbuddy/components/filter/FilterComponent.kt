package ru.slartus.boostbuddy.components.filter

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.value.Value
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.components.BaseComponent

@Stable
interface FilterComponent {
    val viewStates: Value<FilterViewState>
    val dialogSlot: Value<ChildSlot<*, DialogChild>>

    sealed class DialogChild {
        data class Period(val from: Clock, val to: Clock) : DialogChild()
        data class Tags(val tags: List<Any>) : DialogChild()
    }
}

class FilterComponentImpl(
    componentContext: ComponentContext,
) : BaseComponent<FilterViewState, FilterViewAction>(
    componentContext,
    FilterViewState()
), FilterComponent {
    private val dialogNavigation = SlotNavigation<DialogConfig>()
    private val _dialogSlot = childSlot(
        key = "dialogSlot",
        source = dialogNavigation,
        serializer = DialogConfig.serializer(),
        handleBackButton = true,
        childFactory = ::dialogChild
    )
    override val dialogSlot: Value<ChildSlot<*, FilterComponent.DialogChild>> = _dialogSlot

    private fun dialogChild(
        config: DialogConfig,
        componentContext: ComponentContext
    ): FilterComponent.DialogChild =
        when (config) {
            DialogConfig.Period -> FilterComponent.DialogChild.Period(
                from = Clock.System, to = Clock.System
            )

            DialogConfig.Tags -> FilterComponent.DialogChild.Tags(
                emptyList()
            )
        }

    @Serializable
    private sealed class DialogConfig {
        @Serializable
        data object Period : DialogConfig()

        @Serializable
        data object Tags : DialogConfig()
    }
}