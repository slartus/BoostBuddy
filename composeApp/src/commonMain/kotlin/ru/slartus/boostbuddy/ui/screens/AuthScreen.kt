package ru.slartus.boostbuddy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key.Companion.DirectionCenter
import androidx.compose.ui.input.key.Key.Companion.DirectionDown
import androidx.compose.ui.input.key.Key.Companion.DirectionDownLeft
import androidx.compose.ui.input.key.Key.Companion.DirectionDownRight
import androidx.compose.ui.input.key.Key.Companion.DirectionLeft
import androidx.compose.ui.input.key.Key.Companion.DirectionRight
import androidx.compose.ui.input.key.Key.Companion.DirectionUp
import androidx.compose.ui.input.key.Key.Companion.DirectionUpLeft
import androidx.compose.ui.input.key.Key.Companion.DirectionUpRight
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.auth.AuthComponent
import ru.slartus.boostbuddy.components.auth.AuthViewState
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration
import ru.slartus.boostbuddy.ui.common.keyboardAsState
import ru.slartus.boostbuddy.ui.widgets.WebView
import ru.slartus.boostbuddy.utils.Platform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuthScreen(component: AuthComponent) {
    val density = LocalDensity.current

    val platformConfiguration = LocalPlatformConfiguration.current
    var useCursor by remember { mutableStateOf(false) }
    val isKeyboardOpen by keyboardAsState()
    val state by component.viewStates.subscribeAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = CenterVertically
                    ) {
                        Text(
                            text = "Авторизация"
                        )
                    }
                },
                actions = {
                    if (state.mode == AuthViewState.Mode.WebView) {
                        IconButton(onClick = component::onSwitchToPhoneLoginClick) {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = "Вход по телефону"
                            )
                        }
                        if (platformConfiguration.platform == Platform.AndroidTV)
                            IconButton(onClick = { useCursor = !useCursor }) {
                                Icon(
                                    imageVector = Icons.Filled.NorthWest,
                                    contentDescription = "Программный курсор"
                                )
                            }
                        IconButton(onClick = component::onReloadClick) {
                            Icon(
                                imageVector = Icons.Filled.Repeat,
                                contentDescription = "Повторить"
                            )
                        }
                    } else {
                        IconButton(onClick = component::onSwitchToWebViewClick) {
                            Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = "Вход через сайт"
                            )
                        }
                    }
                }

            )
        },
    ) { innerPadding ->
        if (state.mode != AuthViewState.Mode.WebView) {
            PhoneAuthContent(
                state = state,
                component = component,
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
            return@Scaffold
        }
        var cursorPositionX by remember { mutableStateOf(0.dp) }
        var cursorPositionY by remember { mutableStateOf(0.dp) }
        var clickOffset by remember { mutableStateOf<Offset?>(null) }
        val focusRequester = remember { FocusRequester() }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(isKeyboardOpen) {
            if (useCursor) {
                if (isKeyboardOpen)
                    focusRequester.freeFocus()
                else
                    focusRequester.requestFocus()
            }
        }
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .then(
                    if (!useCursor) Modifier
                    else Modifier
                        .focusable()
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { keyEvent ->
                            if (isKeyboardOpen) return@onPreviewKeyEvent false
                            if (!isOwnKeyCode(keyEvent)) return@onPreviewKeyEvent false
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                when (keyEvent.key) {
                                    DirectionUp -> {
                                        val newValue = cursorPositionY - 5.dp
                                        cursorPositionY = cursorPositionY.dec()
                                        newValue >= 0.dp
                                    }

                                    DirectionUpLeft -> {
                                        cursorPositionY = cursorPositionY.dec()
                                        cursorPositionX = cursorPositionX.dec()
                                        true
                                    }

                                    DirectionUpRight -> {
                                        cursorPositionY = cursorPositionY.dec()
                                        cursorPositionX = cursorPositionX.inc()
                                        true
                                    }

                                    DirectionDown -> {
                                        cursorPositionY = cursorPositionY.inc()
                                        true
                                    }

                                    DirectionDownLeft -> {
                                        cursorPositionY = cursorPositionY.inc()
                                        cursorPositionX = cursorPositionX.dec()
                                        true
                                    }

                                    DirectionDownRight -> {
                                        cursorPositionY = cursorPositionY.inc()
                                        cursorPositionX = cursorPositionX.inc()
                                        true
                                    }

                                    DirectionLeft -> {
                                        cursorPositionX = cursorPositionX.dec()
                                        true
                                    }

                                    DirectionRight -> {
                                        cursorPositionX = cursorPositionX.inc()
                                        true
                                    }

                                    DirectionCenter -> {
                                        coroutineScope.launch {
                                            clickOffset = with(density) {
                                                Offset(
                                                    cursorPositionX.toPx(),
                                                    cursorPositionY.toPx()
                                                )
                                            }
                                            delay(500)
                                            if (!isKeyboardOpen)
                                                focusRequester.requestFocus()
                                        }
                                        true
                                    }

                                    else -> false
                                }
                            } else {
                                true
                            }
                        }
                )

        ) {
            WebView(
                state.url,
                clickOffset,
                onCookieChange = component::onCookiesChanged
            )
            if (useCursor)
                Icon(
                    modifier = Modifier.size(24.dp)
                        .clickable {
                            useCursor = false
                        }
                        .offset(x = cursorPositionX, y = cursorPositionY),
                    imageVector = Icons.Filled.NorthWest,
                    tint = Color.Red,
                    contentDescription = "Обновить"
                )
        }
        InfoDialogView()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneAuthContent(
    state: AuthViewState,
    component: AuthComponent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        when (state.mode) {
            AuthViewState.Mode.EnterPhone -> {
                Text("Введите номер телефона")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = component::onPhoneChanged,
                    label = { Text("+7 999 000 00 00") },
                    singleLine = true,
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = component::onSendSmsCodeClick,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("Получить код")
                    }
                }
            }

            AuthViewState.Mode.EnterSmsCode -> {
                Text("Код отправлен. Проверьте сообщения в приложении Boosty, свои мессенджеры или смс")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.smsCode,
                    onValueChange = component::onSmsCodeChanged,
                    label = { Text("Код") },
                    singleLine = true,
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = component::onConfirmSmsCodeClick,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("Войти")
                    }
                }
                Spacer(Modifier.height(8.dp))
                val resendAt = state.resendAvailableAtEpochMs
                val secondsLeft by produceState(
                    initialValue = computeSecondsLeft(resendAt),
                    key1 = resendAt
                ) {
                    while (true) {
                        value = computeSecondsLeft(resendAt)
                        if (value <= 0) break
                        kotlinx.coroutines.delay(1000)
                    }
                }
                if (secondsLeft > 0) {
                    Text("Повторная отправка через $secondsLeft сек")
                } else {
                    OutlinedButton(
                        onClick = component::onResendSmsCodeClick,
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Выслать повторно")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = component::onBackToPhoneClick,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Изменить номер")
                }
            }

            AuthViewState.Mode.WebView -> Unit
        }
        val errorMessage = state.errorMessage
        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = component::onDismissError,
                confirmButton = {
                    Button(onClick = component::onDismissError) { Text("ОК") }
                },
                title = { Text("Ошибка") },
                text = { Text(errorMessage) }
            )
        }
    }
}

@Composable
private fun InfoDialogView() {
    var isDialogOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(2000)
        isDialogOpen = true
    }
    if (isDialogOpen) {
        AlertDialog(
            onDismissRequest = {
                isDialogOpen = false
            },
            confirmButton = {
                Button(onClick = {
                    isDialogOpen = false
                }) {
                    Text("ОК")
                }
            },
            title = { Text("Внимание") },
            text = {
                Text("Необходимо принять соглашение по использованию cookies и залогиниться.\n\nКлиент использует полученный в cookie токен.")
            },
        )
    }
}

private fun computeSecondsLeft(resendAtEpochMs: Long?): Int {
    if (resendAtEpochMs == null) return 0
    val diff = resendAtEpochMs - Clock.System.now().toEpochMilliseconds()
    return if (diff <= 0) 0 else ((diff + 999) / 1000).toInt()
}

private fun Dp.inc(): Dp = this + 5.dp
private fun Dp.dec(): Dp = max(this - 5.dp, 0.dp)

private fun isOwnKeyCode(keyEvent: KeyEvent): Boolean =
    when (keyEvent.key) {
        DirectionUp,
        DirectionUpLeft,
        DirectionUpRight,
        DirectionDown,
        DirectionDownLeft,
        DirectionDownRight,
        DirectionLeft,
        DirectionRight,
        DirectionCenter -> true

        else -> false
    }
