package ru.slartus.boostbuddy.widgets

import androidx.compose.runtime.Composable
import ru.slartus.boostbuddy.components.VideoState

@Composable
expect fun VideoPlayer(url: String, title: String, onVideoStateChange: (VideoState) -> Unit)