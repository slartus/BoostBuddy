package ru.slartus.boostbuddy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.slartus.boostbuddy.components.settings.SettingsComponent
import ru.slartus.boostbuddy.ui.common.BottomView
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration
import ru.slartus.boostbuddy.ui.theme.LocalThemeIsDark

@Composable
internal fun SettingsScreen(component: SettingsComponent) {
    val state by component.viewStates.subscribeAsState()
    val platformConfiguration = LocalPlatformConfiguration.current
    val isDarkState by LocalThemeIsDark.current
    BottomView("Настройки") {
        Column {
            CheckBoxItem(
                text = "Системный видео-плеер",
                checked = state.appSettings.useSystemVideoPlayer,
                onCheckedChange = { newCheckedState ->
                    component.onUseSystemPlayerClicked(newCheckedState)
                }
            )
            TextItem(
                icon = Icons.Default.AttachMoney,
                text = "Поддержать проект",
                onClick = {
                    component.onDonateClicked()
                }
            )

            TextItem(
                icon = if (isDarkState) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                text = if (isDarkState) "Светлая тема" else "Тёмная тема",
                onClick = {
                    component.onSetDarkModeClicked(!isDarkState)
                }
            )

            CheckBoxItem(
                text = "Собирать логи",
                checked = state.appSettings.debugLog,
                onCheckedChange = { newCheckedState ->
                    component.onDebugLogClicked(newCheckedState)
                }
            )

            if (state.appSettings.debugLog) {
                TextItem(
                    icon = Icons.Default.Attachment,
                    text = "Отправить лог",
                    onClick = {
                        component.onSendLogClicked()
                    }
                )
            }

            TextItem(
                icon = Icons.Default.Info,
                text = "Версия программы: ${platformConfiguration.appVersion}",
                onClick = {
                    component.onVersionClicked()
                }
            )
        }
    }
}

@Composable
private fun TextItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = icon,
            contentDescription = "Icon"
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun CheckBoxItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { newCheckedState ->
                onCheckedChange(newCheckedState)
            },
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}