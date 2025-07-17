package ru.slartus.boostbuddy.ui.screens.filter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.collections.immutable.persistentListOf
import ru.slartus.boostbuddy.components.filter.AccessType
import ru.slartus.boostbuddy.components.filter.FilterComponent
import ru.slartus.boostbuddy.ui.common.BottomView
import ru.slartus.boostbuddy.ui.common.TextBottomViewItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterDialogView(
    component: FilterComponent,
    onDismissClicked: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    ModalBottomSheet(
        modifier = Modifier,
        onDismissRequest = { onDismissClicked() },
        sheetState = sheetState
    ) {
        FilterView(component)
    }
}

@Composable
private fun FilterView(component: FilterComponent) {
    val state by component.viewStates.subscribeAsState()
    BottomView("Фильтр") {
        Column {
            val accessTypes =
                remember { persistentListOf(AccessType.Allowed, AccessType.All, AccessType.Bought) }

            accessTypes.forEach { accessType ->
                TextBottomViewItem(
                    text = accessType.title,
                    selected = state.filter.accessType == accessType,
                    onClick = { component.onAccessTypeChange(accessType) }
                )
            }
            Divider(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            )
            TextBottomViewItem(
                text = "Теги",
                onClick = component::onTagsClick,
            )
        }
    }
}

private val AccessType.title: String
    get() = when (this) {
        AccessType.All -> "Все посты"
        AccessType.Allowed -> "Доступные мне"
        AccessType.Bought -> "Только купленные"
    }