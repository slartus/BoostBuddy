package ru.slartus.boostbuddy.ui.screens.qrlogin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.seiko.imageloader.rememberImagePainter
import ru.slartus.boostbuddy.components.qrlogin.QrLoginComponent
import ru.slartus.boostbuddy.components.qrlogin.QrLoginErrorKind
import ru.slartus.boostbuddy.components.qrlogin.QrLoginViewState
import ru.slartus.boostbuddy.ui.common.CenteredScrollableColumn
import ru.slartus.boostbuddy.ui.common.HorizontalSpacer
import ru.slartus.boostbuddy.ui.common.QrCodeImage
import ru.slartus.boostbuddy.ui.common.isTvPlatform
import ru.slartus.boostbuddy.ui.common.rememberTvInitialFocus
import ru.slartus.boostbuddy.ui.common.tvFocusHighlight

private const val TITLE = "Вход по QR-коду"
private val TvQrSize = 280.dp
private val PhoneQrMaxSize = 320.dp
private val PhoneMaxContentWidth = 600.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrLoginScreen(component: QrLoginComponent) {
    val state by component.viewStates.subscribeAsState()
    if (isTvPlatform()) {
        CenteredScrollableColumn(
            modifier = Modifier.padding(horizontal = 48.dp, vertical = 24.dp),
        ) {
            QrLoginContent(state = state, isTv = true, component = component)
        }
    } else {
        Scaffold(
            topBar = { TopAppBar(title = { Text(TITLE) }) },
        ) { innerPadding ->
            CenteredScrollableColumn(
                modifier = Modifier.padding(innerPadding).padding(24.dp),
                maxContentWidth = PhoneMaxContentWidth,
            ) {
                QrLoginContent(state = state, isTv = false, component = component)
            }
        }
    }
}

@Composable
private fun ColumnScope.QrLoginContent(
    state: QrLoginViewState,
    isTv: Boolean,
    component: QrLoginComponent,
) {
    when (state) {
        QrLoginViewState.Starting -> ProgressContent(text = null)
        is QrLoginViewState.ShowQr -> if (isTv) {
            TvShowQrContent(state = state, component = component)
        } else {
            PhoneShowQrContent(state = state, component = component)
        }

        is QrLoginViewState.Confirm -> ConfirmContent(state = state, component = component)
        QrLoginViewState.Completing -> ProgressContent(text = "Входим…")
        QrLoginViewState.Success -> SuccessContent()
        is QrLoginViewState.Error -> ErrorContent(kind = state.kind, component = component)
    }
}

@Composable
private fun ProgressContent(text: String?) {
    CircularProgressIndicator(modifier = Modifier.testTag(QrLoginTestTags.PROGRESS))
    if (text != null) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TvShowQrContent(
    state: QrLoginViewState.ShowQr,
    component: QrLoginComponent,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QrCodeImage(
            modifier = Modifier.testTag(QrLoginTestTags.QR_IMAGE),
            content = state.pageUrl,
            size = TvQrSize,
            contentDescription = "QR-код для входа",
        )
        HorizontalSpacer(48.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = TITLE, style = MaterialTheme.typography.headlineSmall)
            QrInstructions(state = state)
            ShowQrButtons(component = component)
        }
    }
}

@Composable
private fun ColumnScope.PhoneShowQrContent(
    state: QrLoginViewState.ShowQr,
    component: QrLoginComponent,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        QrCodeImage(
            modifier = Modifier.testTag(QrLoginTestTags.QR_IMAGE),
            content = state.pageUrl,
            size = min(maxWidth, PhoneQrMaxSize),
            contentDescription = "QR-код для входа",
        )
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        QrInstructions(state = state)
        ShowQrButtons(component = component)
    }
}

@Composable
private fun QrInstructions(state: QrLoginViewState.ShowQr) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(QrLoginTestTags.STEPS),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QrSteps.forEach { step ->
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = step,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
    Text(
        modifier = Modifier.fillMaxWidth().testTag(QrLoginTestTags.DONOR_WARNING),
        text = "Устройство, с которого вы передадите вход, выйдет из аккаунта Boosty.",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyLarge,
    )
    Text(
        modifier = Modifier.fillMaxWidth().testTag(QrLoginTestTags.STATUS),
        text = "Ожидаем телефон… ${formatTimeLeft(state.secondsLeft)}",
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        modifier = Modifier.fillMaxWidth().testTag(QrLoginTestTags.HOST),
        text = "Адрес: ${state.host}",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun ShowQrButtons(component: QrLoginComponent) {
    val backFocus = rememberTvInitialFocus()
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedButton(
            modifier = Modifier
                .tvFocusHighlight(ButtonDefaults.outlinedShape)
                .testTag(QrLoginTestTags.BUTTON_REFRESH),
            onClick = component::onRefreshQr,
        ) {
            Text("Обновить QR")
        }
        OutlinedButton(
            modifier = Modifier
                .tvFocusHighlight(ButtonDefaults.outlinedShape)
                .focusRequester(backFocus)
                .testTag(QrLoginTestTags.BUTTON_BACK),
            onClick = component::onBack,
        ) {
            Text("Назад")
        }
    }
}

@Composable
private fun ConfirmContent(
    state: QrLoginViewState.Confirm,
    component: QrLoginComponent,
) {
    val confirmFocus = rememberTvInitialFocus()
    val avatarUrl = state.avatarUrl
    if (!avatarUrl.isNullOrEmpty()) {
        Image(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .testTag(QrLoginTestTags.AVATAR),
            painter = rememberImagePainter(avatarUrl),
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
    }
    val profileName = state.profileName
    Text(
        modifier = Modifier.testTag(QrLoginTestTags.CONFIRM_TITLE),
        text = if (profileName.isNullOrBlank()) "Войти в аккаунт Boosty?" else "Войти как $profileName?",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineSmall,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            modifier = Modifier
                .tvFocusHighlight(ButtonDefaults.shape)
                .focusRequester(confirmFocus)
                .testTag(QrLoginTestTags.BUTTON_CONFIRM),
            onClick = component::onConfirm,
        ) {
            Text("Войти")
        }
        OutlinedButton(
            modifier = Modifier
                .tvFocusHighlight(ButtonDefaults.outlinedShape)
                .testTag(QrLoginTestTags.BUTTON_CANCEL),
            onClick = component::onCancel,
        ) {
            Text("Отмена")
        }
    }
}

@Composable
private fun SuccessContent() {
    Icon(
        modifier = Modifier.size(48.dp),
        imageVector = Icons.Filled.CheckCircle,
        tint = MaterialTheme.colorScheme.primary,
        contentDescription = null,
    )
    Text(
        modifier = Modifier.testTag(QrLoginTestTags.SUCCESS),
        text = "Вход выполнен",
        style = MaterialTheme.typography.headlineSmall,
    )
}

@Composable
private fun ErrorContent(
    kind: QrLoginErrorKind,
    component: QrLoginComponent,
) {
    val retryFocus = rememberTvInitialFocus()
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            modifier = Modifier.testTag(QrLoginTestTags.ERROR_MESSAGE),
            text = kind.message(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            modifier = Modifier
                .tvFocusHighlight(ButtonDefaults.shape)
                .focusRequester(retryFocus)
                .testTag(QrLoginTestTags.BUTTON_RETRY),
            onClick = component::onRetry,
        ) {
            Text("Повторить")
        }
        OutlinedButton(
            modifier = Modifier
                .tvFocusHighlight(ButtonDefaults.outlinedShape)
                .testTag(QrLoginTestTags.BUTTON_OTHER_METHOD),
            onClick = component::onOtherMethod,
        ) {
            Text("Другой способ")
        }
    }
}

private val QrSteps = listOf(
    "1. Отсканируйте QR-код камерой телефона.",
    "2. Откройте ссылку и войдите в Boosty в браузере — или передайте вход из приложения BoostBuddy.",
    "3. Подтвердите вход на этом экране.",
)

private fun QrLoginErrorKind.message(): String = when (this) {
    QrLoginErrorKind.NoLan -> "Нет подключения к локальной сети. Подключите устройство к Wi‑Fi или Ethernet."
    QrLoginErrorKind.NoFreePort -> "Не удалось запустить приём: порты заняты. Попробуйте ещё раз."
    QrLoginErrorKind.StartFailed -> "Не удалось запустить приём. Попробуйте ещё раз."
    QrLoginErrorKind.Expired -> "Время ожидания истекло. Обновите QR-код."
    QrLoginErrorKind.TooManyAttempts -> "Слишком много неудачных попыток. Обновите QR-код."
    QrLoginErrorKind.ProfileFailed -> "Не удалось проверить аккаунт. Попробуйте ещё раз."
    QrLoginErrorKind.RefreshNetwork -> "Нет связи с Boosty. Повторите подтверждение."
    QrLoginErrorKind.RefreshRejected ->
        "Boosty отклонил вход. Вход мог быть перенесён частично — при необходимости войдите заново на обоих устройствах."
}

private fun formatTimeLeft(secondsLeft: Int): String {
    val minutes = (secondsLeft / 60).toString().padStart(2, '0')
    val seconds = (secondsLeft % 60).toString().padStart(2, '0')
    return "$minutes:$seconds"
}

internal object QrLoginTestTags {
    private const val NAME = "qr_login"
    const val PROGRESS = "${NAME}_progress"
    const val QR_IMAGE = "${NAME}_qr_image"
    const val STEPS = "${NAME}_steps"
    const val DONOR_WARNING = "${NAME}_donor_warning"
    const val STATUS = "${NAME}_status"
    const val HOST = "${NAME}_host"
    const val BUTTON_REFRESH = "${NAME}_button_refresh"
    const val BUTTON_BACK = "${NAME}_button_back"
    const val AVATAR = "${NAME}_avatar"
    const val CONFIRM_TITLE = "${NAME}_confirm_title"
    const val BUTTON_CONFIRM = "${NAME}_button_confirm"
    const val BUTTON_CANCEL = "${NAME}_button_cancel"
    const val SUCCESS = "${NAME}_success"
    const val ERROR_MESSAGE = "${NAME}_error_message"
    const val BUTTON_RETRY = "${NAME}_button_retry"
    const val BUTTON_OTHER_METHOD = "${NAME}_button_other_method"
}
