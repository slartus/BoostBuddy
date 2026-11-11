package ru.slartus.boostbuddy.ui.screens.pairsend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.slartus.boostbuddy.components.pairsend.PairSendComponent
import ru.slartus.boostbuddy.components.pairsend.PairSendErrorKind
import ru.slartus.boostbuddy.components.pairsend.PairSendViewState
import ru.slartus.boostbuddy.ui.common.CenteredScrollableColumn
import ru.slartus.boostbuddy.ui.common.rememberTvInitialFocus
import ru.slartus.boostbuddy.ui.common.tvFocusHighlight

private val MaxContentWidth = 600.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairSendScreen(component: PairSendComponent) {
    val state by component.viewStates.subscribeAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Передача входа") }) },
    ) { innerPadding ->
        CenteredScrollableColumn(
            modifier = Modifier.padding(innerPadding).padding(24.dp),
            maxContentWidth = MaxContentWidth,
        ) {
            when (val currentState = state) {
                is PairSendViewState.Confirm -> ConfirmContent(host = currentState.host, component = component)
                PairSendViewState.Sending -> SendingContent()
                PairSendViewState.Done -> DoneContent()
                is PairSendViewState.Error -> ErrorContent(kind = currentState.kind, component = component)
            }
        }
    }
}

@Composable
private fun ConfirmContent(
    host: String,
    component: PairSendComponent,
) {
    val cancelFocus = rememberTvInitialFocus()
    Text(
        modifier = Modifier.testTag(PairSendTestTags.CONFIRM_TITLE),
        text = "Передать вход на $host?",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineSmall,
    )
    Text(
        modifier = Modifier.testTag(PairSendTestTags.LOGOUT_WARNING),
        text = "На этом устройстве произойдёт выход из аккаунта.",
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyLarge,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            modifier = Modifier
                .tvFocusHighlight(ButtonDefaults.shape)
                .testTag(PairSendTestTags.BUTTON_SEND),
            onClick = component::onSend,
        ) {
            Text("Передать и выйти")
        }
        OutlinedButton(
            modifier = Modifier
                .tvFocusHighlight(ButtonDefaults.outlinedShape)
                .focusRequester(cancelFocus)
                .testTag(PairSendTestTags.BUTTON_CANCEL),
            onClick = component::onCancel,
        ) {
            Text("Отмена")
        }
    }
}

@Composable
private fun SendingContent() {
    CircularProgressIndicator(modifier = Modifier.testTag(PairSendTestTags.PROGRESS))
    Text(text = "Передаём вход…", style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun DoneContent() {
    Text(
        modifier = Modifier.testTag(PairSendTestTags.DONE_MESSAGE),
        text = "Вход передан. Подтвердите вход на другом устройстве.",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun ErrorContent(
    kind: PairSendErrorKind,
    component: PairSendComponent,
) {
    val isRetryAvailable = kind.isRetryAvailable()
    val initialFocus = rememberTvInitialFocus()
    Text(
        modifier = Modifier.testTag(PairSendTestTags.ERROR_MESSAGE),
        text = kind.message(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleMedium,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isRetryAvailable) {
            Button(
                modifier = Modifier
                    .tvFocusHighlight(ButtonDefaults.shape)
                    .focusRequester(initialFocus)
                    .testTag(PairSendTestTags.BUTTON_RETRY),
                onClick = component::onRetry,
            ) {
                Text("Повторить")
            }
            OutlinedButton(
                modifier = Modifier
                    .tvFocusHighlight(ButtonDefaults.outlinedShape)
                    .testTag(PairSendTestTags.BUTTON_CANCEL),
                onClick = component::onCancel,
            ) {
                Text("Отмена")
            }
        } else {
            Button(
                modifier = Modifier
                    .tvFocusHighlight(ButtonDefaults.shape)
                    .focusRequester(initialFocus)
                    .testTag(PairSendTestTags.BUTTON_CANCEL),
                onClick = component::onCancel,
            ) {
                Text("Закрыть")
            }
        }
    }
}

private fun PairSendErrorKind.isRetryAvailable(): Boolean = when (this) {
    PairSendErrorKind.NotLoggedIn,
    PairSendErrorKind.Rejected,
    PairSendErrorKind.Uncertain -> false

    PairSendErrorKind.Network,
    PairSendErrorKind.Unknown -> true
}

private fun PairSendErrorKind.message(): String = when (this) {
    PairSendErrorKind.NotLoggedIn -> "На этом устройстве нет активного входа, который можно передать."
    PairSendErrorKind.Network ->
        "Не удалось связаться с устройством. Убедитесь, что оба устройства в одной сети Wi‑Fi."
    PairSendErrorKind.Rejected -> "QR-код устарел или уже использован. Обновите QR-код на другом устройстве."
    PairSendErrorKind.Uncertain ->
        "Не удалось подтвердить передачу. Проверьте экран другого устройства: если вход там появился, на этом устройстве выход произойдёт автоматически."
    PairSendErrorKind.Unknown -> "Не удалось передать вход. Попробуйте ещё раз."
}

internal object PairSendTestTags {
    private const val NAME = "pair_send"
    const val CONFIRM_TITLE = "${NAME}_confirm_title"
    const val LOGOUT_WARNING = "${NAME}_logout_warning"
    const val BUTTON_SEND = "${NAME}_button_send"
    const val BUTTON_CANCEL = "${NAME}_button_cancel"
    const val PROGRESS = "${NAME}_progress"
    const val DONE_MESSAGE = "${NAME}_done_message"
    const val ERROR_MESSAGE = "${NAME}_error_message"
    const val BUTTON_RETRY = "${NAME}_button_retry"
}
