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
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import ru.slartus.boostbuddy.components.video.VideoState
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.VideoQuality

private const val SEEK_UPDATE_INTERVAL_MS = 1000L

@Composable
actual fun VideoPlayer(
    vid: String,
    playerUrl: PlayerUrl,
    title: String,
    position: Long,
    onVideoStateChange: (VideoState) -> Unit,
    onContentPositionChange: (Long) -> Unit,
    onStopClick: () -> Unit
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
            seekTo(position)
            playWhenReady = true
            prepare()
        }
        onDispose {
            exoPlayer.release()
        }
    }

    VideoPlayerChrome(
        exoPlayer = exoPlayer,
        title = title,
        playingPosition = playingPosition,
        isEnded = isEnded,
        onStopClick = onStopClick
    )
}

@OptIn(UnstableApi::class)
private fun ExoPlayer.setMediaSource(mediaId: String, title: String, playerUrl: PlayerUrl) {
    when (playerUrl.quality) {
        VideoQuality.HLS -> {
            val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
            val hlsMediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(playerUrl.url))
            setMediaSource(hlsMediaSource)
        }

        VideoQuality.DASH -> {
            val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
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
                })
            }
    }
}

internal fun ExoPlayer.startPlayer() {
    playWhenReady = true
    play()
}

internal fun ExoPlayer.pausePlayer() {
    playWhenReady = false
    pause()
}