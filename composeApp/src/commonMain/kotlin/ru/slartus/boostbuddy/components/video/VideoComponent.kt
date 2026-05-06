package ru.slartus.boostbuddy.components.video

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.components.video.VideoState.Buffering
import ru.slartus.boostbuddy.components.video.VideoState.Ended
import ru.slartus.boostbuddy.components.video.VideoState.Error
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
    fun onRetryClicked()
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
    val playbackError: Boolean = false,
    val retryToken: Int = 0,
)

val Content.OkVideo.timeCodeMs: Long get() = timeCode * 1000

enum class VideoState {
    Idle, Buffering, Ready, Ended, Error
}

internal class VideoComponentImpl(
    componentContext: ComponentContext,
    blogUrl: String,
    postId: String,
    postData: Content.OkVideo,
    playerUrl: PlayerUrl,
    liveBlogUrl: String? = null,
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
    private val liveStreamModel: LiveStreamComponentModel? = liveBlogUrl?.let { url ->
        LiveStreamComponentModel(
            scope = scope,
            streamRepository = streamRepository,
            blogUrl = url,
            postData = postData,
        )
    }

    init {
        if (liveStreamModel != null) {
            viewState = viewState.copy(postData = postData, loading = false)
            timeCodeManager.setContentId(postData.id)
            liveStreamModel.startHeartbeat()
            lifecycle.doOnDestroy { liveStreamModel.stopHeartbeat() }
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
            Idle -> if (!viewState.playbackError) viewState = viewState.copy(loading = true)
            Buffering -> viewState = viewState.copy(loading = true)
            Ready -> {
                viewState = viewState.copy(loading = false, playbackError = false)
                liveStreamModel?.resetDvrFallbackAttempts()
            }
            Ended -> handlePlaybackEnded()
            Error -> {
                if (tryDvrToLiveEdgeFallback()) return
                viewState = viewState.copy(playbackError = true, loading = false)
            }
        }
    }

    /**
     * Если плеер упал в DVR-режиме (например, OK CDN отдал 404 на `_offset_p`
     * URL потому, что DVR-буфер ещё не накопился), автоматически возвращаемся
     * на live edge URL вместо показа экрана ошибки. Сбрасываем live-edge
     * трекинг, чтобы post-fallback `false` события снова считались шумом
     * догоняния буфера. Лимит попыток предотвращает infinite swap loop, если
     * post-fallback controller успеет re-arm флаг до первого Ready.
     */
    private fun tryDvrToLiveEdgeFallback(): Boolean {
        val model = liveStreamModel ?: return false
        if (viewState.isAtLiveEdge) return false
        if (!model.shouldAttemptDvrFallback()) return false
        val liveEdgeUrl =
            model.playerUrlForMode(atEdge = true, quality = viewState.playerUrl.quality)
                ?: return false
        model.resetLiveEdgeTracking()
        viewState = viewState.copy(
            isAtLiveEdge = true,
            playerUrl = liveEdgeUrl,
            playbackError = false,
            loading = true,
        )
        return true
    }

    override fun onRetryClicked() {
        viewState = viewState.copy(
            playbackError = false,
            loading = true,
            retryToken = viewState.retryToken + 1,
        )
    }

    private fun handlePlaybackEnded() {
        val model = liveStreamModel ?: return
        if (viewState.streamEnded) return
        viewState = viewState.copy(streamEnded = true, loading = false)
        model.stopHeartbeat()
    }

    override fun onContentPositionChange(position: Long) {
        if (liveStreamModel != null && viewState.isAtLiveEdge) return
        timeCodeManager.onPositionChanged(position)
    }

    override fun onLiveEdgeChanged(atEdge: Boolean) {
        val model = liveStreamModel ?: return
        val effectiveAtEdge = model.processLiveEdgeChange(atEdge) ?: return
        if (viewState.isAtLiveEdge == effectiveAtEdge) return
        val swapped = model.playerUrlForMode(effectiveAtEdge, viewState.playerUrl.quality)
        viewState = viewState.copy(
            isAtLiveEdge = effectiveAtEdge,
            playerUrl = swapped ?: viewState.playerUrl,
        )
        if (effectiveAtEdge && viewState.playbackSpeed != 1f) {
            scope.launch {
                settingsRepository.setPreferredPlaybackSpeed(1f)
            }
        }
    }

    override fun onStopClicked() {
        if (liveStreamModel != null) {
            liveStreamModel.stopHeartbeat()
            if (!viewState.isAtLiveEdge) {
                timeCodeManager.putLastPosition()
            }
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
        val model = liveStreamModel
        val swapped = model?.playerUrlForMode(
            atEdge = viewState.isAtLiveEdge,
            quality = playerUrl.quality,
        )
        // DVR есть, но для выбранной quality URL не нашёлся — фолбэчимся на live edge,
        // чтобы не рассинхронизировать isAtLiveEdge с реальным URL.
        val fallbackToLiveEdge = model != null &&
                model.hasDvrSupport &&
                swapped == null &&
                !viewState.isAtLiveEdge
        viewState = viewState.copy(
            playerUrl = swapped ?: playerUrl,
            isAtLiveEdge = fallbackToLiveEdge || viewState.isAtLiveEdge,
            settingsSheetVisible = false,
        )
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
}
