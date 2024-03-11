package ru.slartus.boostbuddy.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
actual fun keyboardAsState(): State<Boolean> {
    val isImeVisible by remember { mutableStateOf(false) }

    return rememberUpdatedState(isImeVisible)
}