package ru.slartus.boostbuddy.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.slartus.boostbuddy.components.settings.SettingsComponent
import ru.slartus.boostbuddy.ui.common.BottomView
import ru.slartus.boostbuddy.ui.common.CheckboxListItem
import ru.slartus.boostbuddy.ui.common.IconTextListItem
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration
import ru.slartus.boostbuddy.ui.theme.LocalThemeIsDark

@Composable
internal fun SettingsScreen(component: SettingsComponent) {
    val state by component.viewStates.subscribeAsState()
    val platformConfiguration = LocalPlatformConfiguration.current
    val isDarkState by LocalThemeIsDark.current
    BottomView("Настройки") {
        Column {
            CheckboxListItem(
                text = "Системный видео-плеер",
                checked = state.appSettings.useSystemVideoPlayer,
                onCheckedChange = { newCheckedState ->
                    component.onUseSystemPlayerClicked(newCheckedState)
                }
            )
            IconTextListItem(
                icon = Icons.Default.AttachMoney,
                text = "Поддержать проект",
                onClick = {
                    component.onDonateClicked()
                }
            )

            IconTextListItem(
                icon = if (isDarkState) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                text = if (isDarkState) "Светлая тема" else "Тёмная тема",
                onClick = {
                    component.onSetDarkModeClicked(!isDarkState)
                }
            )

            CheckboxListItem(
                text = "Собирать логи",
                checked = state.appSettings.debugLog,
                onCheckedChange = { newCheckedState ->
                    component.onDebugLogClicked(newCheckedState)
                }
            )

            if (state.appSettings.debugLog) {
                IconTextListItem(
                    icon = Icons.Default.Attachment,
                    text = "Отправить лог",
                    onClick = {
                        component.onSendLogClicked()
                    }
                )
            }

            IconTextListItem(
                icon = Icons.Default.Info,
                text = "Версия программы: ${platformConfiguration.appVersion}",
                onClick = {
                    component.onVersionClicked()
                }
            )
        }
    }
}