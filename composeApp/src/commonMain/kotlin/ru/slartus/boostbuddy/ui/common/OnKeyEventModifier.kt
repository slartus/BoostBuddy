package ru.slartus.boostbuddy.ui.common

import androidx.compose.ui.Modifier

expect fun Modifier.onAppKeyEvent(onKeyEvent: (AppKeyEvent) -> Boolean): Modifier

data class AppKeyEvent(val action: AppKeyEventAction, val keyCode: AppKeyEventKeyCode)

enum class AppKeyEventAction {
    Down, Any
}

enum class AppKeyEventKeyCode {
    DpadLeft, DpadRight, Any
}