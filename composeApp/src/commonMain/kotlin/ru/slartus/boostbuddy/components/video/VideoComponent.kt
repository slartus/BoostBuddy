package ru.slartus.boostbuddy.components.video

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val postTitle: String? = null,
    val playerUrl: PlayerUrl,
    val loading: Boolean = true,
    val settingsSheetVisible: Boolean = false,
    val playbackSpeed: Float = 1f,
    val isLive: Boolean = false,
    val liveStartedAtSeconds: Long? = null,
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
    private val blogUrl: String,
    private val postId: String,
    postData: Content.OkVideo,
    playerUrl: PlayerUrl,
    private val liveBlogUrl: String? = null,
    liveStartedAtSeconds: Long? = null,
    private val onStopClicked: () -> Unit
) : BaseComponent<VideoViewState, Any>(
    componentContext,
    VideoViewState(
        postData = null,
        playerUrl = playerUrl,
        isLive = liveBlogUrl != null,
        liveStartedAtSeconds = liveStartedAtSeconds,
    )
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
        )
    }
    private var bufferingDebounceJob: Job? = null
    private var autoRecoveryAttempted = false
    private var recoverJob: Job? = null

    init {
        if (liveStreamModel != null) {
            viewState = viewState.copy(
                postData = postData,
                postTitle = postData.title,
                loading = false,
            )
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
                    enforceLiveSpeed()
                }
        }
    }

    // Live-стрим всегда играет на 1x. Sync state update + async settings write —
    // иначе round-trip через settings flow даёт лаг ~100-300ms, юзер видит 3x
    // (унаследовано из VOD-сессии) до сброса.
    private fun enforceLiveSpeed() {
        liveStreamModel ?: return
        if (viewState.playbackSpeed == 1f) return
        viewState = viewState.copy(playbackSpeed = 1f)
        scope.launch {
            settingsRepository.setPreferredPlaybackSpeed(1f)
        }
    }

    override fun onVideoStateChanged(videoState: VideoState) {
        when (videoState) {
            Idle -> {
                cancelLoadingDebounce()
                if (!viewState.playbackError) viewState = viewState.copy(loading = true)
            }

            Buffering -> showBufferingLoadingDebounced()
            Ready -> {
                cancelLoadingDebounce()
                autoRecoveryAttempted = false
                viewState = viewState.copy(loading = false, playbackError = false)
            }

            Ended -> {
                cancelLoadingDebounce()
                handlePlaybackEnded()
            }
            Error -> {
                cancelLoadingDebounce()
                if (autoRecoveryAttempted) {
                    viewState = viewState.copy(playbackError = true, loading = false)
                } else {
                    viewState = viewState.copy(loading = true)
                    recoverPlayback()
                }
            }
        }
    }

    // Тонкий буфер у HLS live даёт частые короткие BUFFERING→READY циклы между
    // сегментами — без дебаунса лоадер мигает на каждом сегменте.
    private fun showBufferingLoadingDebounced() {
        if (viewState.loading) return
        bufferingDebounceJob?.cancel()
        bufferingDebounceJob = scope.launch {
            delay(LOADING_DEBOUNCE_MS)
            viewState = viewState.copy(loading = true)
        }
    }

    private fun cancelLoadingDebounce() {
        bufferingDebounceJob?.cancel()
        bufferingDebounceJob = null
    }

    override fun onRetryClicked() {
        viewState = viewState.copy(playbackError = false, loading = true)
        recoverPlayback()
    }

    private fun recoverPlayback() {
        autoRecoveryAttempted = true
        recoverJob?.cancel()
        recoverJob = scope.launch {
            val fresh = fetchFreshPlayback()
            viewState = if (fresh != null) {
                viewState.copy(
                    postData = fresh.first,
                    playerUrl = fresh.second,
                    playbackError = false,
                    loading = true,
                    retryToken = viewState.retryToken + 1,
                )
            } else {
                viewState.copy(playbackError = true, loading = false)
            }
        }
    }

    private suspend fun fetchFreshPlayback(): Pair<Content.OkVideo, PlayerUrl>? {
        val preferred = viewState.playerUrl.quality
        val video = if (liveBlogUrl != null) {
            streamRepository.fetchActive(liveBlogUrl).getOrNull()?.video
        } else {
            val videoId = viewState.postData?.id ?: return null
            postRepository.getPost(blogUrl, postId).getOrNull()
                ?.data?.filterIsInstance<Content.OkVideo>()
                ?.find { it.id == videoId }
        } ?: return null
        val freshUrl = video.playerUrls.pickPlayerUrl(preferred) ?: return null
        return video to freshUrl
    }

    private fun handlePlaybackEnded() {
        val model = liveStreamModel ?: return
        if (viewState.streamEnded) return
        viewState = viewState.copy(streamEnded = true, loading = false)
        model.stopHeartbeat()
    }

    override fun onContentPositionChange(position: Long) {
        if (liveStreamModel != null) return
        timeCodeManager.onPositionChanged(position)
    }

    override fun onStopClicked() {
        if (liveStreamModel != null) {
            liveStreamModel.stopHeartbeat()
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
        viewState = viewState.copy(
            playerUrl = playerUrl,
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
            val post = postRepository.getPost(blogUrl, postId).getOrNull()
            val refreshData = post?.data
                ?.filterIsInstance<Content.OkVideo>()
                ?.find { it.id == postData.id }
                ?: postData
            timeCodeManager.setContentId(refreshData.id)
            viewState = viewState.copy(
                postData = refreshData,
                postTitle = post?.title,
                loading = false,
            )
        }
    }

    private companion object {
        private const val LOADING_DEBOUNCE_MS = 500L
    }
}
