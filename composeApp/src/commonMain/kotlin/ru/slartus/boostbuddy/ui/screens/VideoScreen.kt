package ru.slartus.boostbuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import ru.slartus.boostbuddy.components.video.VideoComponent
import ru.slartus.boostbuddy.components.video.timeCodeMs
import ru.slartus.boostbuddy.ui.common.HideSystemBarsEffect
import ru.slartus.boostbuddy.ui.common.KeepScreenOnEffect
import ru.slartus.boostbuddy.ui.widgets.VideoPlayer

@Composable
internal fun VideoScreen(component: VideoComponent) {
    val state by component.viewStates.subscribeAsState()

    KeepScreenOnEffect()
    HideSystemBarsEffect()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        state.postData?.let { postData ->
            VideoPlayer(
                vid = postData.vid,
                playerUrl = state.playerUrl,
                title = postData.title,
                position = postData.timeCodeMs,
                onVideoStateChange = { state -> component.onVideoStateChanged(state) },
                onContentPositionChange = { component.onContentPositionChange(it) },
                onStopClick = { component.onStopClicked() }
            )
        }

        if (state.loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(58.dp)
            )
        }
    }
}