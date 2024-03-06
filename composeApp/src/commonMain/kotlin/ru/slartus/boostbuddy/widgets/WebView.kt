package ru.slartus.boostbuddy.widgets

import androidx.compose.runtime.Composable

@Composable
expect fun WebView(url: String, onCookieChange: (String) -> Unit)