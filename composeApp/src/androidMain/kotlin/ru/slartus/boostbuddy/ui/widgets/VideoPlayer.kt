package ru.slartus.boostbuddy.ui.widgets

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import ru.slartus.boostbuddy.components.video.VideoState

@UnstableApi
@Composable
actual fun VideoPlayer(
    vid: String,
    url: String,
    title: String,
    position: Long,
    onVideoStateChange: (VideoState) -> Unit,
    onContentPositionChange: (Long) -> Unit,
    onStopClick: () -> Unit
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

    var contentPosition by remember { mutableLongStateOf(0L) }
    LaunchedEffect(contentPosition) {
        onContentPositionChange(contentPosition)
    }
    val exoPlayer = remember {
        runCatching {
            ExoPlayer.Builder(context).build().apply {
                setMediaItems(mediaItems)
                prepare()
                addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) {
                        super.onEvents(player, events)

                        contentPosition = player.contentPosition
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
            exoPlayer.seekTo(position)
            exoPlayer.playWhenReady = true
            onDispose {
                exoPlayer.release()
            }
        }

        LocalLifecycleOwner.current.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        exoPlayer.pause()
                    }

                    else -> Unit
                }
            }
        })


        val playerView = remember {
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
        }



        var shouldShowController by remember { mutableStateOf(false) }
        AndroidView(
            modifier = Modifier
                .focusable()
                .onKeyEvent { keyEvent ->
                    if (!isOwnKeyCode(keyEvent)) return@onKeyEvent false
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT,
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                            KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> {
                                exoPlayer.seekBack()
                                true
                            }

                            KeyEvent.KEYCODE_DPAD_RIGHT,
                            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                            KeyEvent.KEYCODE_MEDIA_NEXT,
                            KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> {
                                exoPlayer.seekForward()
                                true
                            }

                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                                shouldShowController = shouldShowController.not()
                                when {
                                    exoPlayer.isPlaying -> {
                                        shouldShowController = true
                                        exoPlayer.pausePlayer()
                                    }

                                    else -> {
                                        shouldShowController = false
                                        exoPlayer.startPlayer()
                                    }
                                }
                                true
                            }

                            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                                shouldShowController = false
                                exoPlayer.startPlayer()
                                true
                            }

                            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                                shouldShowController = true
                                exoPlayer.pausePlayer()
                                true
                            }

                            KeyEvent.KEYCODE_MEDIA_STOP -> {
                                onStopClick()
                                true
                            }

                            else -> {
                                false
                            }
                        }
                    } else {
                        true
                    }

                }
                .fillMaxSize(),
            factory = {
                playerView
            },
            update = {
                if (shouldShowController) it.showController()
                else it.hideController()
            }
        )

    }
}

private fun ExoPlayer.startPlayer() {
    playWhenReady = true
    play()
}

private fun ExoPlayer.pausePlayer() {
    playWhenReady = false
    pause()
}

private fun isOwnKeyCode(keyEvent: androidx.compose.ui.input.key.KeyEvent): Boolean =
    when (keyEvent.nativeKeyEvent.keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        KeyEvent.KEYCODE_MEDIA_PLAY,
        KeyEvent.KEYCODE_MEDIA_STOP,
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
        KeyEvent.KEYCODE_MEDIA_NEXT,
        KeyEvent.KEYCODE_MEDIA_PREVIOUS,
        KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD,
        KeyEvent.KEYCODE_MEDIA_STEP_FORWARD,
        KeyEvent.KEYCODE_MEDIA_PAUSE -> true

        else -> {
            false
        }
    }
