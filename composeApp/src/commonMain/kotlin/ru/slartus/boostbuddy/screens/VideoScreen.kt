package ru.slartus.boostbuddy.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import ru.slartus.boostbuddy.components.VideoComponent
import ru.slartus.boostbuddy.widgets.VideoPlayer

@Composable
fun VideoScreen(component: VideoComponent) {
    val state = component.state.subscribeAsState().value

    val playerUrl by remember(state.playerUrl) {
        mutableStateOf(state.playerUrl?.url)
    }

    if (playerUrl != null) {
        VideoPlayer(playerUrl!!)
    }

}