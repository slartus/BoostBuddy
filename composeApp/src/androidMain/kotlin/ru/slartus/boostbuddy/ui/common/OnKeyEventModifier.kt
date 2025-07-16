package ru.slartus.boostbuddy.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent

actual fun Modifier.onAppKeyEvent(onKeyEvent: (AppKeyEvent) -> Boolean): Modifier =
    this.onKeyEvent { event ->
        onKeyEvent(AppKeyEvent(
            action = event.nativeKeyEvent.action.toKeyEventAction(),
            keyCode= event.nativeKeyEvent.keyCode.toKeyEventKeyCode()
        ))
    }

private fun Int.toKeyEventAction(): AppKeyEventAction = when (this) {
    android.view.KeyEvent.ACTION_DOWN -> AppKeyEventAction.Down
    else -> AppKeyEventAction.Any
}

private fun Int.toKeyEventKeyCode(): AppKeyEventKeyCode = when (this) {
    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> AppKeyEventKeyCode.DpadLeft
    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> AppKeyEventKeyCode.DpadRight
    else -> AppKeyEventKeyCode.Any
}