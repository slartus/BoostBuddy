package ru.slartus.boostbuddy.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.slartus.boostbuddy.widgets.WebView
import ru.slartus.boostbuddy.components.AuthComponent

@Composable
fun AuthScreen(component: AuthComponent) {
    Column {
        Text(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            text = "Авторизация"
        )
        WebView("https://boosty.to", onCookieChange = component::onCookiesChanged)
    }
}
