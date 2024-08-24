package ru.slartus.boostbuddy.ui.widgets

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun NavigationComponent(
    items: List<NavigationItem>,
    onItemClick: (NavigationItem) -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            items.forEach { item ->
                item(
                    icon = {
                        Icon(
                            item.icon,
                            contentDescription = item.label
                        )
                    },
                    label = { Text(item.label) },
                    selected = item.selected,
                    onClick = { onItemClick(item) }
                )
            }
        }
    ) {
        content()
    }
}