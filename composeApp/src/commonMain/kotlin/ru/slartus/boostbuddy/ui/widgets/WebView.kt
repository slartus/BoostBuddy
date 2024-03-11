package ru.slartus.boostbuddy.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset

@Composable
expect fun WebView(url: String, clickCoors: Offset?, onCookieChange: (String) -> Unit)