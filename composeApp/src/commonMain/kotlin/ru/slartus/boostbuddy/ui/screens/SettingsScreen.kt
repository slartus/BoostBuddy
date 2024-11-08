package ru.slartus.boostbuddy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.slartus.boostbuddy.components.settings.SettingsComponent
import ru.slartus.boostbuddy.ui.common.BottomView
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration

@Composable
internal fun SettingsScreen(component: SettingsComponent) {
    val state by component.viewStates.subscribeAsState()
    val platformConfiguration = LocalPlatformConfiguration.current
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
                text = "Поддержать проект",
                onClick = {
                    component.onDonateClicked()
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
                    text = "Отправить лог",
                    onClick = {
                        component.onSendLogClicked()
                    }
                )
            }

            TextItem(
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
    text: String,
    onClick: () -> Unit = {}
) {
    Text(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        text = text,
        style = MaterialTheme.typography.bodyMedium
    )
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