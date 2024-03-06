package ru.slartus.boostbuddy.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import ru.slartus.boostbuddy.components.VideoState.*
import ru.slartus.boostbuddy.data.repositories.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.PostData

interface VideoComponent {
    val state: Value<VideoViewState>
    fun onVideoStateChanged(videoState: VideoState)
}

data class VideoViewState(
    val postData: PostData,
    val loading: Boolean = false
) {
    val playerUrl: PlayerUrl? = postData.videoUrls?.firstOrNull { it.type == "hls" }
}

enum class VideoState {
    Idle, Buffering, Ready, Ended
}

class VideoComponentImpl(
    componentContext: ComponentContext,
    private val postData: PostData
) : VideoComponent, ComponentContext by componentContext {
    private val scope = coroutineScope()

    private val _state = MutableValue(VideoViewState(postData))
    override var state: Value<VideoViewState> = _state
    override fun onVideoStateChanged(videoState: VideoState) {
        when (videoState) {
            Idle -> _state.value = VideoViewState(postData, loading = true)
            Buffering -> _state.value = VideoViewState(postData, loading = true)
            Ready -> _state.value = VideoViewState(postData, loading = false)
            Ended -> {

            }
        }
    }
}