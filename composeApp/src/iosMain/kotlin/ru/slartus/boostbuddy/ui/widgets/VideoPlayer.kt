package ru.slartus.boostbuddy.ui.widgets

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import ru.slartus.boostbuddy.components.video.VideoState

@Composable
actual fun VideoPlayer(
    vid: String,
    url: String,
    title: String,
    position: Long,
    onVideoStateChange: (VideoState) -> Unit,
    onContentPositionChange: (Long) -> Unit,
    onStopClick: () -> Unit
) {
    Text(text = "Not implemented")
}