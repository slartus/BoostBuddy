package ru.slartus.boostbuddy.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import ru.slartus.boostbuddy.components.VideoState.Buffering
import ru.slartus.boostbuddy.components.VideoState.Ended
import ru.slartus.boostbuddy.components.VideoState.Idle
import ru.slartus.boostbuddy.components.VideoState.Ready
import ru.slartus.boostbuddy.data.repositories.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.PostData

interface VideoComponent {
    val viewStates: Value<VideoViewState>
    fun onVideoStateChanged(videoState: VideoState)
}

data class VideoViewState(
    val postData: PostData,
    val playerUrl: PlayerUrl,
    val loading: Boolean = false
)

enum class VideoState {
    Idle, Buffering, Ready, Ended
}

class VideoComponentImpl(
    componentContext: ComponentContext,
    postData: PostData,
    playerUrl: PlayerUrl
) : BaseComponent<VideoViewState>(componentContext, VideoViewState(postData, playerUrl)), VideoComponent {
    override fun onVideoStateChanged(videoState: VideoState) {
        when (videoState) {
            Idle -> viewState = viewState.copy(loading = true)
            Buffering -> viewState = viewState.copy(loading = true)
            Ready -> viewState = viewState.copy(loading = false)
            Ended -> Unit
        }
    }
}