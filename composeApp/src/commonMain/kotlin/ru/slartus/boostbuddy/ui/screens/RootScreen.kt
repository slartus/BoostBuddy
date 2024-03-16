package ru.slartus.boostbuddy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import ru.slartus.boostbuddy.components.RootComponent
import ru.slartus.boostbuddy.ui.screens.blog.BlogScreen
import ru.slartus.boostbuddy.ui.theme.AppTheme
import ru.slartus.boostbuddy.ui.theme.LocalThemeIsDark


@Composable
fun RootScreen(component: RootComponent, modifier: Modifier = Modifier) {
    val state by component.viewStates.subscribeAsState()
    AppTheme(state.darkMode) {
        var isDarkState by LocalThemeIsDark.current
        isDarkState = state.darkMode ?: isDarkState
        Children(
            stack = component.stack,
            modifier = modifier,
            animation = stackAnimation(fade()),
        ) {
            when (val child = it.instance) {
                is RootComponent.Child.AuthChild -> AuthScreen(child.component)
                is RootComponent.Child.SubscribesChild -> SubscribesScreen(child.component)
                is RootComponent.Child.BlogChild -> BlogScreen(child.component)
                is RootComponent.Child.VideoChild -> VideoScreen(child.component)
            }
        }

        val dialogSlot by component.dialogSlot.subscribeAsState()
        dialogSlot.child?.instance?.also { dialogComponent ->
            when (dialogComponent) {
                is RootComponent.DialogChild.NewVersion ->
                    NewVersionDialogView(
                        version = dialogComponent.version,
                        info = dialogComponent.info,
                        onAcceptClicked = {
                            component.onDialogVersionAcceptClicked(dialogComponent)
                        },
                        onCancelClicked = {
                            component.onDialogVersionCancelClicked()
                        },
                        onDismissClicked = {
                            component.onDialogVersionDismissed()
                        },
                    )

                is RootComponent.DialogChild.Error -> ErrorDialogView(
                    error = dialogComponent.message,
                    onDismissClicked =  {
                        component.onDialogErrorDismissed()
                    },
                )
            }
        }
    }
}

@Composable
private fun ErrorDialogView(
    error: String,
    onDismissClicked: () -> Unit,
) {
    var isDialogOpen by remember { mutableStateOf(true) }
    if (isDialogOpen) {
        AlertDialog(
            onDismissRequest = {
                isDialogOpen = false
                onDismissClicked()
            },
            confirmButton = {
                Button(onClick = {
                    isDialogOpen = false
                    onDismissClicked()
                }) {
                    Text("OK")
                }
            },
            title = { Text("Произошла ошибка") },
            text = { Text(error) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewVersionDialogView(
    version: String,
    info: String,
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
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Найдена новая версия приложения",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Найдена новая версия: $version",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = info,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Text(
                modifier = Modifier.fillMaxWidth().clickable { onAcceptClicked() }
                    .padding(vertical = 16.dp),
                text = "Скачать и установить",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                modifier = Modifier.fillMaxWidth().clickable { onCancelClicked() }
                    .padding(vertical = 16.dp),
                text = "Отложить",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}