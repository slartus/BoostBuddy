package ru.slartus.boostbuddy.ui.widgets

import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.common.PlaybackException
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import ru.slartus.boostbuddy.components.video.VideoState
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.VideoQuality
import java.io.IOException

private const val SEEK_UPDATE_INTERVAL_MS = 1000L
private const val OK_DVR_PATH_MARKER = "_offset_p"

// `live_*` (sliding window) и `live_playback_*` (`_offset_p`, абсолютный timeline)
// у OK CDN используют разные timeline-якоря. Сохранённая позиция из одного
// манифеста почти наверняка окажется вне seekable range другого и вызовет
// Source error при seek — поэтому при пересечении этой границы переключаемся
// на seekToDefaultPosition нового источника.
private fun isOkLiveDvrSwap(previousUrl: String, newUrl: String): Boolean =
    previousUrl.contains(OK_DVR_PATH_MARKER) != newUrl.contains(OK_DVR_PATH_MARKER)

private class PlayerUrlHolder(var value: PlayerUrl)

@Composable
actual fun VideoPlayer(
    vid: String,
    playerUrl: PlayerUrl,
    title: String,
    position: Long,
    playbackSpeed: Float,
    isLive: Boolean,
    isAtLiveEdge: Boolean,
    retryToken: Int,
    onVideoStateChange: (VideoState) -> Unit,
    onContentPositionChange: (Long) -> Unit,
    onLiveEdgeChanged: (Boolean) -> Unit,
    onStopClick: () -> Unit,
    onSettingsClick: (() -> Unit)?
) {
    var playingPosition by remember { mutableLongStateOf(0L) }

    var isEnded by remember { mutableStateOf(false) }

    val exoPlayer = rememberPlayer(
        onVideoStateChange = { state ->
            onVideoStateChange(state)
            isEnded = state == VideoState.Ended
        },
        onContentPositionChange = {
            playingPosition = it
            onContentPositionChange(it)
        }
    )

    LaunchedEffect(exoPlayer) {
        while (isActive) {
            playingPosition = exoPlayer.contentPosition
            delay(SEEK_UPDATE_INTERVAL_MS)
        }
    }

    exoPlayer.observeLifeCycle()

    DisposableEffect(exoPlayer) {
        exoPlayer.apply {
            setMediaSource(mediaId = vid, title = title, playerUrl = playerUrl)
            if (isLive && position <= 0L) {
                seekToDefaultPosition()
            } else {
                seekTo(position)
            }
            playWhenReady = true
            prepare()
        }
        onDispose {
            exoPlayer.release()
        }
    }

    val lastPlayerUrl = remember(exoPlayer) { PlayerUrlHolder(playerUrl) }
    LaunchedEffect(exoPlayer, playerUrl) {
        if (playerUrl == lastPlayerUrl.value) return@LaunchedEffect
        val previousUrl = lastPlayerUrl.value.url
        lastPlayerUrl.value = playerUrl
        val savedPosition = exoPlayer.currentPosition
        val wasPlaying = exoPlayer.playWhenReady
        val crossesLiveDvrBoundary = isOkLiveDvrSwap(previousUrl, playerUrl.url)
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaSource(mediaId = vid, title = title, playerUrl = playerUrl)
        if (crossesLiveDvrBoundary) {
            // OK CDN: live_* (sliding window) и live_playback_* (`_offset_p`,
            // абсолютный timeline) — разные timeline-якоря. savedPosition из
            // одного манифеста легко окажется вне seekable range другого
            // и вызовет Source error. Стартуем новый источник на его default
            // position (live edge для live URL, начало seekable окна для DVR).
            exoPlayer.seekToDefaultPosition()
        } else {
            exoPlayer.seekTo(savedPosition)
        }
        exoPlayer.playWhenReady = wasPlaying
        exoPlayer.prepare()
    }

    LaunchedEffect(exoPlayer, playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    LaunchedEffect(exoPlayer, retryToken) {
        if (retryToken > 0 && exoPlayer.playbackState == Player.STATE_IDLE) exoPlayer.prepare()
    }

    VideoPlayerChrome(
        exoPlayer = exoPlayer,
        title = title,
        playingPosition = playingPosition,
        isEnded = isEnded,
        isLive = isLive,
        isAtLiveEdge = isAtLiveEdge,
        onLiveEdgeChanged = onLiveEdgeChanged,
        onStopClick = onStopClick,
        onSettingsClick = onSettingsClick,
    )
}

@OptIn(UnstableApi::class)
private fun ExoPlayer.setMediaSource(mediaId: String, title: String, playerUrl: PlayerUrl) {
    when (playerUrl.quality) {
        VideoQuality.HLS -> {
            val dataSourceFactory: DataSource.Factory = loggingHttpFactory()
            val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(playerUrl.url))
            setMediaSource(hlsMediaSource)
        }

        VideoQuality.DASH -> {
            val dataSourceFactory: DataSource.Factory = loggingHttpFactory()
            val dashMediaSource = DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(playerUrl.url))
            setMediaSource(dashMediaSource)
        }

        else -> setMediaItem(
            MediaItem.Builder()
                .setUri(playerUrl.url)
                .setMediaId(mediaId)
                .setTag(playerUrl.url)
                .setMediaMetadata(
                    MediaMetadata.Builder().setDisplayTitle(title).build()
                )
                .build()
        )
    }
}

@SuppressLint("ComposableNaming")
@Composable
private fun ExoPlayer.observeLifeCycle() {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(this, lifecycleOwner) {
        val observer = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_PAUSE,
                    Lifecycle.Event.ON_STOP -> pause()

                    else -> Unit
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun rememberPlayer(
    onVideoStateChange: (VideoState) -> Unit,
    onContentPositionChange: (Long) -> Unit,
): ExoPlayer {
    val context = LocalContext.current
    return remember {
        ExoPlayer.Builder(context)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) {
                        super.onEvents(player, events)
                        onContentPositionChange(player.contentPosition)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        when (playbackState) {
                            ExoPlayer.STATE_IDLE -> onVideoStateChange(VideoState.Idle)
                            ExoPlayer.STATE_BUFFERING -> onVideoStateChange(VideoState.Buffering)
                            ExoPlayer.STATE_READY -> onVideoStateChange(VideoState.Ready)
                            ExoPlayer.STATE_ENDED -> onVideoStateChange(VideoState.Ended)
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)
                        onVideoStateChange(VideoState.Error)
                    }
                })
                addAnalyticsListener(LoadDiagnosticsListener)
            }
    }
}

@OptIn(UnstableApi::class)
private object LoadDiagnosticsListener : AnalyticsListener {
    override fun onLoadStarted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
        retryCount: Int
    ) {
        Napier.d(tag = LOG_TAG) {
            "load start dataType=${mediaLoadData.dataType} retry=$retryCount uri=${loadEventInfo.uri}"
        }
    }

    override fun onLoadError(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
        error: IOException,
        wasCanceled: Boolean
    ) {
        val httpEx = error as? HttpDataSource.InvalidResponseCodeException
        Napier.e(tag = LOG_TAG) {
            buildString {
                append("load error dataType=").append(mediaLoadData.dataType)
                append(" uri=").append(loadEventInfo.uri)
                if (httpEx != null) {
                    append(" status=").append(httpEx.responseCode)
                    append(" headers=").append(httpEx.headerFields)
                } else {
                    append(" exception=").append(error.javaClass.simpleName)
                    append(" msg=").append(error.message)
                }
                append(" canceled=").append(wasCanceled)
            }
        }
    }
}

@OptIn(UnstableApi::class)
private fun loggingHttpFactory(): DataSource.Factory {
    val delegate = DefaultHttpDataSource.Factory()
    return DataSource.Factory {
        val source = delegate.createDataSource()
        LoggingHttpDataSource(source)
    }
}

@OptIn(UnstableApi::class)
private class LoggingHttpDataSource(
    private val delegate: HttpDataSource,
) : HttpDataSource by delegate {
    override fun open(dataSpec: DataSpec): Long {
        Napier.d(tag = LOG_TAG) {
            "http open uri=${dataSpec.uri} range=${dataSpec.position}-${dataSpec.length} reqHeaders=${dataSpec.httpRequestHeaders}"
        }
        return try {
            val len = delegate.open(dataSpec)
            Napier.d(tag = LOG_TAG) {
                "http opened uri=${dataSpec.uri} contentLen=$len respHeaders=${delegate.responseHeaders}"
            }
            len
        } catch (e: HttpDataSource.InvalidResponseCodeException) {
            Napier.e(tag = LOG_TAG) {
                "http fail uri=${dataSpec.uri} status=${e.responseCode} respHeaders=${e.headerFields}"
            }
            throw e
        }
    }
}

private const val LOG_TAG = "VideoPlayer"

internal fun ExoPlayer.startPlayer() {
    playWhenReady = true
    play()
}

internal fun ExoPlayer.pausePlayer() {
    playWhenReady = false
    pause()
}