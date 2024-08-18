package ru.slartus.boostbuddy.ui.screens.main

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.slartus.boostbuddy.components.main.MainComponent
import ru.slartus.boostbuddy.components.main.MainViewNavigationItem
import ru.slartus.boostbuddy.components.main.title


@Composable
internal fun NavigationDrawer(
    activeComponent: MainComponent.Child,
    drawerState: DrawerState,
    onItemClick: (MainViewNavigationItem) -> Unit,
    content: @Composable () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(4.dp)
            ) {
                MainViewNavigationItem.entries.forEach { item ->
                    val selected = activeComponent.navigationItem == item
                    val contentColor =
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    NavigationDrawerItem(
                        modifier = Modifier
                            .then(
                                if (selected) Modifier.focusRequester(focusRequester)
                                else Modifier
                            ),
                        shape = RoundedCornerShape(4.dp),
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                tint = contentColor,
                                contentDescription = item.title,
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                color = contentColor,
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        selected = selected,
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
    LaunchedEffect(drawerState) {
        if (drawerState.isOpen)
            focusRequester.requestFocus()
    }
}

private val MainViewNavigationItem.icon: ImageVector
    get() = when (this) {
        MainViewNavigationItem.Feed -> Icons.Default.RssFeed
        MainViewNavigationItem.Subscribes -> Icons.Default.Subscriptions
    }