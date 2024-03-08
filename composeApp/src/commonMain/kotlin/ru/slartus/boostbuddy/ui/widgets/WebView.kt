package ru.slartus.boostbuddy.ui.widgets

import androidx.compose.runtime.Composable

@Composable
expect fun WebView(url: String, onCookieChange: (String) -> Unit)