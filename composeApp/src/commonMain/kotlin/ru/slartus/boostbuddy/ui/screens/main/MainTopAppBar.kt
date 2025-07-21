package ru.slartus.boostbuddy.ui.screens.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ru.slartus.boostbuddy.components.top_bar.TopBarComponent
import ru.slartus.boostbuddy.ui.screens.TopAppBar

@Composable
internal fun MainTopAppBar(
    component: TopBarComponent,
    onMenuClick: () -> Unit
) {
    var showDropDownMenu by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Text(text = "Лента")
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Меню"
                )
            }
        },
        actions = {
            AppDropdownMenu(
                expanded = showDropDownMenu,
                onDismiss = { showDropDownMenu = false },
                onSettingsClick = component::onSettingsClicked,
                onFeedbackClick = component::onFeedbackClicked,
                onLogoutClick = component::onLogoutClicked
            )
            DropdownMenuTrigger(
                onClick = { showDropDownMenu = true }
            )
        },
        onRefreshClick = component::onRefreshClicked,
        onSearchQueryChange = component::onSearchQueryChange,
        onFilterClick = component::onFilterClicked,
    )
}

@Composable
private fun DropdownMenuTrigger(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Filled.MoreVert, "Дополнительные действия")
    }
}

@Composable
private fun AppDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Настройки") },
            leadingIcon = { Icon(Icons.Filled.Settings, "Settings") },
            onClick = {
                onDismiss()
                onSettingsClick()
            }
        )
        DropdownMenuItem(
            text = { Text("Обсудить на форуме") },
            leadingIcon = { Icon(Icons.Filled.Feedback, "Feedback") },
            onClick = {
                onDismiss()
                onFeedbackClick()
            }
        )
        DropdownMenuItem(
            text = { Text("Выйти из аккаунта") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, "Logout") },
            onClick = {
                onDismiss()
                onLogoutClick()
            }
        )
    }
}