package ru.slartus.boostbuddy.ui.screens

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.auth.AuthComponent
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration
import ru.slartus.boostbuddy.ui.widgets.WebView
import ru.slartus.boostbuddy.utils.Platform

@Composable
fun AuthScreen(component: AuthComponent) {
    val density = LocalDensity.current

    val platformConfiguration = LocalPlatformConfiguration.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                text = "Ожидание авторизационной куки"
            )
        }

        var cursorPositionX by remember { mutableStateOf(0.dp) }
        var cursorPositionY by remember { mutableStateOf(0.dp) }
        var clickOffset by remember { mutableStateOf<Offset?>(null) }
        val focusRequester = remember { FocusRequester() }
        val coroutineScope = rememberCoroutineScope()
        Box(
            modifier = Modifier
                .then(
                    when (platformConfiguration.platform) {
                        Platform.Android, Platform.iOS -> Modifier
                        Platform.AndroidTV -> Modifier
                            .focusable()
                            .focusRequester(focusRequester)
                            .onPreviewKeyEvent { keyEvent ->
                                if (!isOwnKeyCode(keyEvent)) return@onPreviewKeyEvent false
                                if (keyEvent.type == KeyEventType.KeyDown) {
                                    when (keyEvent.key) {
                                        DirectionUp -> {
                                            cursorPositionY = max(cursorPositionY - 5.dp, 0.dp)
                                            true
                                        }

                                        DirectionUpLeft -> {
                                            cursorPositionY = max(cursorPositionY - 5.dp, 0.dp)
                                            cursorPositionX = max(cursorPositionX - 5.dp, 0.dp)
                                            true
                                        }

                                        DirectionUpRight -> {
                                            cursorPositionY = max(cursorPositionY - 5.dp, 0.dp)
                                            cursorPositionX = max(cursorPositionX + 5.dp, 0.dp)
                                            true
                                        }

                                        DirectionDown -> {
                                            cursorPositionY = max(cursorPositionY + 5.dp, 0.dp)
                                            true
                                        }

                                        DirectionDownLeft -> {
                                            cursorPositionY = max(cursorPositionY + 5.dp, 0.dp)
                                            cursorPositionX = max(cursorPositionX - 5.dp, 0.dp)
                                            true
                                        }

                                        DirectionDownRight -> {
                                            cursorPositionY = max(cursorPositionY + 5.dp, 0.dp)
                                            cursorPositionX = max(cursorPositionX + 5.dp, 0.dp)
                                            true
                                        }

                                        DirectionLeft -> {
                                            cursorPositionX = max(cursorPositionX - 5.dp, 0.dp)
                                            true
                                        }

                                        DirectionRight -> {
                                            cursorPositionX = max(cursorPositionX + 5.dp, 0.dp)
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
                    }
                )

        ) {
            WebView("https://boosty.to", clickOffset, onCookieChange = component::onCookiesChanged)
            Icon(
                modifier = Modifier.size(24.dp).offset(x = cursorPositionX, y = cursorPositionY),
                imageVector = Icons.Filled.NorthWest,
                tint = Color.Red,
                contentDescription = "Обновить"
            )
        }
    }
}


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
