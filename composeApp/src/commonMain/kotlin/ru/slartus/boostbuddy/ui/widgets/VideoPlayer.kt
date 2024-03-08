package ru.slartus.boostbuddy.ui.widgets

import androidx.compose.runtime.Composable
import ru.slartus.boostbuddy.components.VideoState

@Composable
expect fun VideoPlayer(
    vid: String,
    url: String,
    title: String,
    position: Long,
    onVideoStateChange: (VideoState) -> Unit,
    onContentPositionChange: (Long) -> Unit
)