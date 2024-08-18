package ru.slartus.boostbuddy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.slartus.boostbuddy.components.top_bar.TopBarComponent
import ru.slartus.boostbuddy.ui.theme.LocalThemeIsDark

@Composable
internal fun TopAppBar(component: TopBarComponent){
    val isDarkState by LocalThemeIsDark.current
    TopAppBar(
        onRefreshClick = { component.onRefreshClicked() },
        onChangeDarkModeClick = { component.onSetDarkModeClicked(!isDarkState) },
        onLogoutClick = { component.onLogoutClicked() },
        onFeedbackClick = { component.onFeedbackClicked() },
        onSettingsClick = { component.onSettingsClicked() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBar(
    onRefreshClick: () -> Unit,
    onChangeDarkModeClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val isDarkState by LocalThemeIsDark.current
    var showDropDownMenu by remember { mutableStateOf(false) }
    androidx.compose.material3.TopAppBar(
        title = { Text("Подписки") },
        actions = {
            IconButton(onClick = { onRefreshClick() }) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Обновить"
                )
            }
            IconButton(onClick = { onChangeDarkModeClick() }) {
                if (!isDarkState) {
                    Icon(
                        imageVector = Icons.Filled.DarkMode,
                        contentDescription = "Тёмная тема"
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.LightMode,
                        contentDescription = "Светлая тема"
                    )
                }
            }
            IconButton(
                onClick = { showDropDownMenu = true }) {
                Icon(Icons.Filled.MoreVert, null)
            }

            DropdownMenu(
                showDropDownMenu, { showDropDownMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(text = "Настройки") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    onClick = {
                        showDropDownMenu = false
                        onSettingsClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = "Обсудить на форуме") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Feedback,
                            contentDescription = "Feedback"
                        )
                    },
                    onClick = {
                        showDropDownMenu = false
                        onFeedbackClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = "Выйти из аккаунта") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout"
                        )
                    },
                    onClick = {
                        showDropDownMenu = false
                        onLogoutClick()
                    }
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogoutDialogView(
    onAcceptClicked: () -> Unit,
    onCancelClicked: () -> Unit,
    onDismissClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = { onDismissClicked() },
        sheetState = sheetState
    ) {
        Column {
            Text(
                modifier = Modifier.fillMaxWidth()
                    .clickable { onAcceptClicked() }
                    .padding(16.dp),
                text = "Выйти из аккаунта"
            )
            Text(
                modifier = Modifier.fillMaxWidth()
                    .clickable { onCancelClicked() }
                    .padding(16.dp),
                text = "Отмена"
            )
        }
    }
}