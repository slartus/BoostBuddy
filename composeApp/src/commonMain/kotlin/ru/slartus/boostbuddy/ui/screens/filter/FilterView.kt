package ru.slartus.boostbuddy.ui.screens.filter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.collections.immutable.persistentListOf
import ru.slartus.boostbuddy.components.filter.AccessType
import ru.slartus.boostbuddy.components.filter.FilterComponent
import ru.slartus.boostbuddy.components.filter.FilterViewState
import ru.slartus.boostbuddy.ui.common.BottomView
import ru.slartus.boostbuddy.ui.common.IconTextListItem
import ru.slartus.boostbuddy.ui.widgets.DateRangePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterDialogView(
    component: FilterComponent,
    onDismissClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val state by component.viewStates.subscribeAsState()
    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissClicked,
        sheetState = sheetState
    ) {
        FilterContent(component, state)
    }
    val dialogSlot by component.dialogSlot.subscribeAsState()
    dialogSlot.child?.instance?.let { dialogComponent ->
        when (dialogComponent) {
            is FilterComponent.DialogChild.Period -> DateRangePickerDialog(
                initialFrom = state.filter.period?.from,
                initialTo = state.filter.period?.to,
                onDateRangeSelected = { from, to ->
                    component.onDateRangeSelected(from, to)
                    component.onDialogDismissed()
                },
                onDismiss = component::onDialogDismissed,
                onReset = component::onDateRangeReset
            )

            is FilterComponent.DialogChild.Tags -> FilterTagsDialogView(
                component = dialogComponent.component,
                onDismissClicked = component::onDialogDismissed
            )
        }
    }
}

@Composable
private fun FilterContent(
    component: FilterComponent,
    state: FilterViewState,
    modifier: Modifier = Modifier
) {

    BottomView(
        modifier = modifier,
        title = "Фильтр",
        trailingContent = {
            if (!state.filter.isEmpty) {
                TextButton(
                    onClick = component::onResetClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Сбросить")
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            AccessTypeSection(
                currentAccessType = state.filter.accessType,
                onAccessTypeChange = component::onAccessTypeChange
            )

            DividerSection()

            TagsSection(
                tagsText = state.tagsText,
                onTagsClick = component::onTagsClick
            )

            IconTextListItem(
                text = state.dateRangeText,
                icon = Icons.Default.DateRange,
                onClick = { component.onPeriodClick() }
            )
        }
    }
}

@Composable
private fun AccessTypeSection(
    currentAccessType: AccessType,
    onAccessTypeChange: (AccessType) -> Unit
) {
    val accessTypes = remember {
        persistentListOf(
            AccessTypeItem(AccessType.Allowed, "Доступные мне", Icons.Default.Person),
            AccessTypeItem(AccessType.All, "Все посты", Icons.Default.Public),
            AccessTypeItem(AccessType.Bought, "Только купленные", Icons.Default.ShoppingCart)
        )
    }

    accessTypes.forEach { item ->
        IconTextListItem(
            text = item.title,
            icon = item.icon,
            selected = currentAccessType == item.type,
            onClick = { onAccessTypeChange(item.type) }
        )
    }
}

@Composable
private fun DividerSection() {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun TagsSection(
    tagsText: String,
    onTagsClick: () -> Unit
) {
    IconTextListItem(
        text = tagsText,
        icon = Icons.AutoMirrored.Filled.Label,
        onClick = onTagsClick
    )
}

private data class AccessTypeItem(
    val type: AccessType,
    val title: String,
    val icon: ImageVector
)