package ru.slartus.boostbuddy.ui.widgets

import androidx.compose.runtime.Composable
import ru.slartus.boostbuddy.components.video.VideoState
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl

@Composable
expect fun VideoPlayer(
    vid: String,
    playerUrl: PlayerUrl,
    title: String,
    position: Long,
    onVideoStateChange: (VideoState) -> Unit,
    onContentPositionChange: (Long) -> Unit,
    onStopClick: () -> Unit
)