package ru.slartus.boostbuddy.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import ru.slartus.boostbuddy.components.VideoComponent
import ru.slartus.boostbuddy.utils.HideSystemBarsEffect
import ru.slartus.boostbuddy.utils.KeepScreenOnEffect
import ru.slartus.boostbuddy.widgets.VideoPlayer

@Composable
fun VideoScreen(component: VideoComponent) {
    val state = component.viewStates.subscribeAsState().value

    val playerUrl by remember(state.playerUrl) {
        mutableStateOf(state.playerUrl?.url)
    }
    KeepScreenOnEffect()
    HideSystemBarsEffect()
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        playerUrl?.let { url ->
            VideoPlayer(
                url = url,
                title = state.postData.title,
                onVideoStateChange = { state -> component.onVideoStateChanged(state) })
        }

        if (state.loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(58.dp)
            )
        }
    }

}