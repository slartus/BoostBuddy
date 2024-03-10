package ru.slartus.boostbuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import ru.slartus.boostbuddy.components.video.VideoComponent
import ru.slartus.boostbuddy.ui.common.HideSystemBarsEffect
import ru.slartus.boostbuddy.ui.common.KeepScreenOnEffect
import ru.slartus.boostbuddy.ui.widgets.VideoPlayer

@Composable
fun VideoScreen(component: VideoComponent) {
    val state by component.viewStates.subscribeAsState()

    val playerUrl by remember(state.playerUrl) {
        mutableStateOf(state.playerUrl.url)
    }
    KeepScreenOnEffect()
    HideSystemBarsEffect()
    Surface(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.fillMaxSize()) {
            state.position?.let { position ->
                VideoPlayer(
                    vid = state.postData.vid,
                    url = playerUrl,
                    title = state.postData.title,
                    position = position,
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

}