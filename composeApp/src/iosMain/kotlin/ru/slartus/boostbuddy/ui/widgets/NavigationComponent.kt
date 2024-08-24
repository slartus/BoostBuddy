package ru.slartus.boostbuddy.ui.widgets

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun NavigationComponent(
    items: List<NavigationItem>,
    onItemClick: (NavigationItem) -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(4.dp)
            ) {
                items.forEach { item ->
                    val contentColor =
                        if (item.selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    NavigationDrawerItem(
                        modifier = Modifier,
                        shape = RoundedCornerShape(4.dp),
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                tint = contentColor,
                                contentDescription = item.label,
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                color = contentColor,
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        selected = item.selected,
                        onClick = {
                            onItemClick(item)
                        }
                    )
                }
            }
        },
    ) {
        content()
    }
}