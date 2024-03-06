package ru.slartus.boostbuddy.widgets

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource

@Composable
actual fun VideoPlayer(url: String) {
    // Get the current context
    val context = LocalContext.current

    // Mutable state to control the visibility of the video title
    val visible = remember { mutableStateOf(true) }

    // Mutable state to hold the video title
    val videoTitle = remember { mutableStateOf("video.name") }

    // Create a list of MediaItems for the ExoPlayer
    val mediaItems = arrayListOf<MediaItem>()
    mediaItems.add(
        MediaItem.Builder()
            .setUri(url)
            .setMediaId("video.id.toString()")
            .setTag(url)
            .setMediaMetadata(MediaMetadata.Builder().setDisplayTitle("video.name").build())
            .build()
    )

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            this.setMediaItems(mediaItems)
            this.prepare()
            this.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    super.onEvents(player, events)
                    // Hide video title after playing for 200 milliseconds
                    if (player.contentPosition >= 200) visible.value = false
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    // Callback when the video changes
                    //onVideoChange(this@apply.currentPeriodIndex)
                    visible.value = true
                    videoTitle.value = mediaItem?.mediaMetadata?.displayTitle.toString()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    // Callback when the video playback state changes to STATE_ENDED
                    if (playbackState == ExoPlayer.STATE_ENDED) {
                        // isVideoEnded.invoke(true)
                    }
                }
            })
        }
    }

    DisposableEffect(Unit) {
        exoPlayer.seekTo(0, C.TIME_UNSET)
        exoPlayer.playWhenReady = true
        onDispose {
            exoPlayer.release()
        }
    }

    // Seek to the specified index and start playing


    // Add a lifecycle observer to manage player state based on lifecycle events
    LocalLifecycleOwner.current.lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_START -> {
                    // Start playing when the Composable is in the foreground
                    if (exoPlayer.isPlaying.not()) {
                        exoPlayer.play()
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    // Pause the player when the Composable is in the background
                    exoPlayer.pause()
                }

                else -> {
                    // Nothing
                }
            }
        }
    })

    // Column Composable to contain the video player
    Column(modifier = Modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                // AndroidView to embed a PlayerView into Compose
                PlayerView(context).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Set resize mode to fill the available space
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                    // Hide unnecessary player controls
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowFastForwardButton(false)
                    setShowRewindButton(false)
                }
            })
        // DisposableEffect to release the ExoPlayer when the Composable is disposed

    }

}