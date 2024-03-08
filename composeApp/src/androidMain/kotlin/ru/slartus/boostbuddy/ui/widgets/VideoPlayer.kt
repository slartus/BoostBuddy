package ru.slartus.boostbuddy.ui.widgets

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import ru.slartus.boostbuddy.components.VideoState

@UnstableApi
@Composable
actual fun VideoPlayer(
    vid: String,
    url: String,
    title: String,
    onVideoStateChange: (VideoState) -> Unit
) {
    val context = LocalContext.current

    val mediaItems = arrayListOf<MediaItem>()
    mediaItems.add(
        MediaItem.Builder()
            .setUri(url)
            .setMediaId(vid)
            .setTag(url)
            .setMediaMetadata(MediaMetadata.Builder().setDisplayTitle(title).build())
            .build()
    )

    var error by remember { mutableStateOf<String?>(null) }

    val exoPlayer = remember {
        runCatching {
            ExoPlayer.Builder(context).build().apply {
                this.setMediaItems(mediaItems)
                this.prepare()
                this.addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) {
                        super.onEvents(player, events)
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
        }.onFailure {
            error = it.toString()
        }.getOrNull()
    }

    error?.let {
        Text(text = it)
    }
    if (exoPlayer != null) {
        DisposableEffect(Unit) {
            exoPlayer.seekTo(0, C.TIME_UNSET)
            exoPlayer.playWhenReady = true
            onDispose {
                exoPlayer.release()
            }
        }

        LocalLifecycleOwner.current.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        if (exoPlayer.isPlaying.not()) {
                            exoPlayer.play()
                        }
                    }

                    Lifecycle.Event.ON_STOP -> {
                        exoPlayer.pause()
                    }

                    else -> Unit
                }
            }
        })

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowFastForwardButton(false)
                    setShowRewindButton(false)
                }
            })
    }
}