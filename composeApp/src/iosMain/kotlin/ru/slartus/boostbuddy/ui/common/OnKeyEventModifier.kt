package ru.slartus.boostbuddy.ui.common

import androidx.compose.ui.Modifier

actual fun Modifier.onAppKeyEvent(onKeyEvent: (AppKeyEvent) -> Boolean): Modifier =
    this