package ru.slartus.boostbuddy.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

@Composable
expect fun keyboardAsState(): State<Boolean>