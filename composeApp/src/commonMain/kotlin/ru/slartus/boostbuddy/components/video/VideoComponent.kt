package ru.slartus.boostbuddy.components.video

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.components.video.VideoState.Buffering
import ru.slartus.boostbuddy.components.video.VideoState.Ended
import ru.slartus.boostbuddy.components.video.VideoState.Idle
import ru.slartus.boostbuddy.components.video.VideoState.Ready
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.PostRepository
import ru.slartus.boostbuddy.data.repositories.VideoRepository
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl

@Stable
interface VideoComponent {
    val viewStates: Value<VideoViewState>
    fun onVideoStateChanged(videoState: VideoState)
    fun onContentPositionChange(position: Long)
    fun onStopClicked()
}

data class VideoViewState(
    val postData: Content.OkVideo?,
    val playerUrl: PlayerUrl,
    val loading: Boolean = true
)

val Content.OkVideo.timeCodeMs: Long get() = timeCode * 1000

enum class VideoState {
    Idle, Buffering, Ready, Ended
}

internal class VideoComponentImpl(
    componentContext: ComponentContext,
    blogUrl: String,
    postId: String,
    postData: Content.OkVideo,
    playerUrl: PlayerUrl,
    private val onStopClicked: () -> Unit
) : BaseComponent<VideoViewState, Any>(componentContext, VideoViewState(null, playerUrl)),
    VideoComponent {

    private val videoRepository by Inject.lazy<VideoRepository>()
    private val postRepository by Inject.lazy<PostRepository>()
    private val timeCodeManager = TimeCodeManager(
        scope = scope,
        videoRepository = videoRepository,
    )

    init {
        refreshData(
            blogUrl = blogUrl,
            postId = postId,
            postData = postData
        )
    }

    override fun onVideoStateChanged(videoState: VideoState) {
        when (videoState) {
            Idle -> viewState = viewState.copy(loading = true)
            Buffering -> viewState = viewState.copy(loading = true)
            Ready -> viewState = viewState.copy(loading = false)
            Ended -> Unit
        }
    }

    override fun onContentPositionChange(position: Long) {
        timeCodeManager.onPositionChanged(position)
    }

    override fun onStopClicked() {
        timeCodeManager.putLastPosition()
        onStopClicked.invoke()
    }

    private fun refreshData(
        blogUrl: String,
        postId: String,
        postData: Content.OkVideo,
    ) {
        scope.launch {
            val postResult = postRepository.getPost(blogUrl, postId)
            val refreshData = postResult.getOrNull()?.let { post ->
                post.data.filterIsInstance<Content.OkVideo>().find { it.id == postData.id }
            } ?: postData
            timeCodeManager.setContentId(refreshData.id)
            viewState = viewState.copy(postData = refreshData, loading = false)
        }
    }
}