package ru.slartus.boostbuddy.screens

import androidx.compose.runtime.Composable
import ru.slartus.boostbuddy.widgets.WebView
import ru.slartus.boostbuddy.components.AuthComponent

@Composable
fun AuthScreen(component: AuthComponent){
    WebView("https://boosty.to", onCookieChange = component::onCookiesChanged)
}
