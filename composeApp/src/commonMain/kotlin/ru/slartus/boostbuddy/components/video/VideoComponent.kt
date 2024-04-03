package ru.slartus.boostbuddy.components.video

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.components.video.VideoState.Buffering
import ru.slartus.boostbuddy.components.video.VideoState.Ended
import ru.slartus.boostbuddy.components.video.VideoState.Idle
import ru.slartus.boostbuddy.components.video.VideoState.Ready
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl

interface VideoComponent {
    val viewStates: Value<VideoViewState>
    fun onVideoStateChanged(videoState: VideoState)
    fun onContentPositionChange(position: Long)
    fun onStopClicked()
}

data class VideoViewState(
    val postData: Content.OkVideo,
    val playerUrl: PlayerUrl,
    val loading: Boolean = false,
    val position: Long? = null,
)

enum class VideoState {
    Idle, Buffering, Ready, Ended
}

class VideoComponentImpl(
    componentContext: ComponentContext,
    postData: Content.OkVideo,
    playerUrl: PlayerUrl,
    private val onStopClicked: () -> Unit
) : BaseComponent<VideoViewState, Any>(componentContext, VideoViewState(postData, playerUrl)),
    VideoComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()

    init {
        scope.launch {
            val position = settingsRepository.getLong(positionKey(viewState.postData.vid)) ?: 0
            viewState = viewState.copy(position = position)
        }
    }

    override fun onVideoStateChanged(videoState: VideoState) {
        when (videoState) {
            Idle -> viewState = viewState.copy(loading = true)
            Buffering -> viewState = viewState.copy(loading = true)
            Ready -> viewState = viewState.copy(loading = false)
            Ended -> {
                scope.launch {
                    settingsRepository.putLong(positionKey(viewState.postData.vid), 0)
                }
            }
        }
    }

    override fun onContentPositionChange(position: Long) {
        scope.launch {
            settingsRepository.putLong(positionKey(viewState.postData.vid), position)
        }
    }

    override fun onStopClicked() {
        onStopClicked.invoke()
    }

    companion object {
        private fun positionKey(vid: String): String = "video_position_$vid"
    }
}