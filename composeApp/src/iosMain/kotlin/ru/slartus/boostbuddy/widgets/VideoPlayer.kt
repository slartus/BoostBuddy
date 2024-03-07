package ru.slartus.boostbuddy.widgets

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import ru.slartus.boostbuddy.components.VideoState

@Composable
actual fun VideoPlayer(vid: String, url: String, title: String, onVideoStateChange: (VideoState) -> Unit) {
    Text(text = "Not implemented")
}