package ru.slartus.boostbuddy.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.VideoState.Buffering
import ru.slartus.boostbuddy.components.VideoState.Ended
import ru.slartus.boostbuddy.components.VideoState.Idle
import ru.slartus.boostbuddy.components.VideoState.Ready
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.PostData

interface VideoComponent {
    val viewStates: Value<VideoViewState>
    fun onVideoStateChanged(videoState: VideoState)
    fun onContentPositionChange(position: Long)
}

data class VideoViewState(
    val postData: PostData,
    val playerUrl: PlayerUrl,
    val loading: Boolean = false,
    val position: Long? = null,
)

enum class VideoState {
    Idle, Buffering, Ready, Ended
}

class VideoComponentImpl(
    componentContext: ComponentContext,
    postData: PostData,
    playerUrl: PlayerUrl
) : BaseComponent<VideoViewState>(componentContext, VideoViewState(postData, playerUrl)),
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

    companion object {
        private fun positionKey(vid: String): String = "video_position_$vid"
    }
}