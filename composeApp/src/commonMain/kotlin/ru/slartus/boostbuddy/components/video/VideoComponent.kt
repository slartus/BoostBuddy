package ru.slartus.boostbuddy.components.video

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.components.video.VideoState.Buffering
import ru.slartus.boostbuddy.components.video.VideoState.Ended
import ru.slartus.boostbuddy.components.video.VideoState.Idle
import ru.slartus.boostbuddy.components.video.VideoState.Ready
import ru.slartus.boostbuddy.components.blog.text
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.PostRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.StreamRepository
import ru.slartus.boostbuddy.data.repositories.VideoRepository
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.utils.PlatformConfiguration

@Stable
interface VideoComponent {
    val viewStates: Value<VideoViewState>
    fun onVideoStateChanged(videoState: VideoState)
    fun onContentPositionChange(position: Long)
    fun onLiveEdgeChanged(atEdge: Boolean)
    fun onStopClicked()
    fun onSettingsClicked()
    fun onSettingsSheetDismissed()
    fun onQualityItemClicked(playerUrl: PlayerUrl)
    fun onPlaybackSpeedSelected(speed: Float)
    fun onDownloadQualitySelected(playerUrl: PlayerUrl)
}

data class VideoViewState(
    val postData: Content.OkVideo?,
    val playerUrl: PlayerUrl,
    val loading: Boolean = true,
    val settingsSheetVisible: Boolean = false,
    val playbackSpeed: Float = 1f,
    val isLive: Boolean = false,
    val isAtLiveEdge: Boolean = true,
    val streamEnded: Boolean = false,
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
    private val liveBlogUrl: String? = null,
    private val onStopClicked: () -> Unit
) : BaseComponent<VideoViewState, Any>(
    componentContext,
    VideoViewState(postData = null, playerUrl = playerUrl, isLive = liveBlogUrl != null)
), VideoComponent {

    private val videoRepository by Inject.lazy<VideoRepository>()
    private val postRepository by Inject.lazy<PostRepository>()
    private val streamRepository by Inject.lazy<StreamRepository>()
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val platformConfiguration by Inject.lazy<PlatformConfiguration>()
    private val timeCodeManager = TimeCodeManager(
        scope = scope,
        videoRepository = videoRepository,
    )
    private var heartbeatJob: Job? = null
    private var heartbeatStopped: Boolean = false

    init {
        if (liveBlogUrl != null) {
            viewState = viewState.copy(postData = postData, loading = false)
            startHeartbeat(liveBlogUrl)
            lifecycle.doOnDestroy {
                if (!heartbeatStopped) {
                    stopHeartbeat(liveBlogUrl)
                }
            }
        } else {
            refreshData(
                blogUrl = blogUrl,
                postId = postId,
                postData = postData
            )
        }
        subscribePlaybackSpeed()
    }

    private fun subscribePlaybackSpeed() {
        scope.launch {
            settingsRepository.appSettingsFlow
                .map { it.preferredPlaybackSpeed }
                .distinctUntilChanged()
                .collect { speed ->
                    viewState = viewState.copy(playbackSpeed = speed)
                }
        }
    }

    override fun onVideoStateChanged(videoState: VideoState) {
        when (videoState) {
            Idle -> viewState = viewState.copy(loading = true)
            Buffering -> viewState = viewState.copy(loading = true)
            Ready -> viewState = viewState.copy(loading = false)
            Ended -> handlePlaybackEnded()
        }
    }

    private fun handlePlaybackEnded() {
        val blogUrl = liveBlogUrl ?: return
        if (viewState.streamEnded) return
        viewState = viewState.copy(streamEnded = true, loading = false)
        stopHeartbeat(blogUrl)
    }

    override fun onContentPositionChange(position: Long) {
        if (liveBlogUrl != null) return
        timeCodeManager.onPositionChanged(position)
    }

    override fun onLiveEdgeChanged(atEdge: Boolean) {
        if (viewState.isAtLiveEdge == atEdge) return
        viewState = viewState.copy(isAtLiveEdge = atEdge)
        if (atEdge && viewState.playbackSpeed != 1f) {
            scope.launch {
                settingsRepository.setPreferredPlaybackSpeed(1f)
            }
        }
    }

    override fun onStopClicked() {
        if (liveBlogUrl != null) {
            stopHeartbeat(liveBlogUrl)
        } else {
            timeCodeManager.putLastPosition()
        }
        onStopClicked.invoke()
    }

    override fun onSettingsClicked() {
        viewState = viewState.copy(settingsSheetVisible = true)
    }

    override fun onSettingsSheetDismissed() {
        viewState = viewState.copy(settingsSheetVisible = false)
    }

    override fun onQualityItemClicked(playerUrl: PlayerUrl) {
        scope.launch {
            settingsRepository.setPreferredQuality(playerUrl.quality)
        }
        viewState = viewState.copy(playerUrl = playerUrl, settingsSheetVisible = false)
    }

    override fun onPlaybackSpeedSelected(speed: Float) {
        // Запись в Settings — единственная точка истины. State обновится через
        // subscribePlaybackSpeed-коллектор, иначе будут два writer'а на одно поле.
        scope.launch {
            settingsRepository.setPreferredPlaybackSpeed(speed)
        }
    }

    override fun onDownloadQualitySelected(playerUrl: PlayerUrl) {
        val title = viewState.postData?.title.orEmpty()
        val fileName = buildDownloadFileName(title, playerUrl)
        platformConfiguration.downloadVideo(
            url = playerUrl.url,
            fileName = fileName,
            onError = null,
        )
        viewState = viewState.copy(settingsSheetVisible = false)
    }

    private fun buildDownloadFileName(title: String, playerUrl: PlayerUrl): String {
        val safeTitle = title
            .replace(Regex("[\\\\/:*?\"<>|\\r\\n\\t]"), "_")
            .trim()
            .ifEmpty { "video" }
            .take(80)
        return "${safeTitle}_${playerUrl.quality.text}.mp4"
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

    private fun startHeartbeat(blogUrl: String) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                streamRepository.heartbeat(blogUrl, stop = false)
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private fun stopHeartbeat(blogUrl: String) {
        if (heartbeatStopped) return
        heartbeatStopped = true
        heartbeatJob?.cancel()
        heartbeatJob = null
        sendStopHeartbeat(blogUrl)
    }

    private fun sendStopHeartbeat(blogUrl: String) {
        externalHeartbeatScope.launch(NonCancellable) {
            streamRepository.heartbeat(blogUrl, stop = true)
        }
    }

    private companion object {
        private const val HEARTBEAT_INTERVAL_MS = 30_000L
        private val externalHeartbeatScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}