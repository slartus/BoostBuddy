package ru.slartus.boostbuddy.ui.common

import androidx.compose.runtime.Composable

@Composable
expect fun BackHandlerEffect(enabled: Boolean, block: () -> Unit)