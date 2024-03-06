package ru.slartus.boostbuddy.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import ru.slartus.boostbuddy.components.VideoComponent
import ru.slartus.boostbuddy.widgets.VideoPlayer

@Composable
fun VideoScreen(component: VideoComponent) {
    val state = component.state.subscribeAsState().value
    Column {
        Text(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            text = "Видео"
        )

        val playerUrl by remember(state.playerUrl) {
            mutableStateOf(state.playerUrl?.url)
        }

        if (playerUrl != null) {
            VideoPlayer(playerUrl!!)
        } else {
            Text(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                text = "None"
            )
        }

    }
}