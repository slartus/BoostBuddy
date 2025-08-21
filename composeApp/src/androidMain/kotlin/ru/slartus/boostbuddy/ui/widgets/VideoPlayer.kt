package ru.slartus.boostbuddy.ui.widgets

import android.graphics.Color
import android.os.SystemClock
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.video.VideoState
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.VideoQuality
import ru.slartus.boostbuddy.ui.common.noRippleClickable
import ru.slartus.boostbuddy.ui.theme.LightColorScheme
import java.util.Locale
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

private val HIDE_CONTROLLER_DELAY = 5.seconds
private val SEEK_INCREMENT = 5.seconds

@UnstableApi
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
    val coroutineScope = rememberCoroutineScope()
    var playingPosition by remember { mutableLongStateOf(0L) }
    val exoPlayer = rememberPlayer(
        onVideoStateChange = onVideoStateChange,
        onContentPositionChange = {
            playingPosition = it
            onContentPositionChange(it)
        }
    )
    LaunchedEffect(Unit) {
        launch {
            while (true) {
                playingPosition = exoPlayer.contentPosition
                delay(1.seconds)
            }
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

    var shouldShowController by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var hideControllerJob: Job by remember { mutableStateOf(Job(null)) }
    var changePositionJob: Job by remember { mutableStateOf(Job(null)) }
    val hideJobActive = hideControllerJob.isActive
    val hideControllersDelayed: () -> Unit = remember {
        {
            hideControllerJob.cancel()
            hideControllerJob = coroutineScope.launch(SupervisorJob()) {
                runCatching {
                    delay(HIDE_CONTROLLER_DELAY)
                    shouldShowController = false
                }
            }
        }
    }
    val showControllerTimed: () -> Unit = remember {
        {
            shouldShowController = true
            hideControllersDelayed()
        }
    }
    val showController: () -> Unit = remember {
        {
            hideControllerJob.cancel()
            shouldShowController = true
        }
    }

    val seekState by remember { mutableStateOf(SeekState()) }
    Box {
        AndroidView(
            modifier = Modifier
                .focusable()
                .focusRequester(focusRequester)
                .onPlayerKeyEvent(
                    onUpClick = {
                        showControllerTimed()
                    },
                    onPauseClick = {
                        showController()
                        exoPlayer.pausePlayer()
                    },
                    onPlayClick = {
                        hideControllersDelayed()
                        exoPlayer.startPlayer()
                    },
                    onPlayPauseClick = {
                        if (exoPlayer.isPlaying) {
                            showController()
                            exoPlayer.pausePlayer()
                        } else {
                            hideControllersDelayed()
                            exoPlayer.startPlayer()
                        }
                    },
                    onLeftClick = { longPress ->
                        val seekMultiplier = seekState.calcSeekMultiplier(longPress)
                        exoPlayer.seekTo(
                            exoPlayer.currentPosition - (SEEK_INCREMENT.toLong(
                                DurationUnit.MILLISECONDS
                            ) * seekMultiplier).toLong()
                        )
                        showControllerTimed()
                    },
                    onRightClick = { longPress ->
                        val seekMultiplier = seekState.calcSeekMultiplier(longPress)
                        exoPlayer.seekTo(
                            exoPlayer.currentPosition + (SEEK_INCREMENT.toLong(
                                DurationUnit.MILLISECONDS
                            ) * seekMultiplier).toLong()
                        )
                        showControllerTimed()
                    },
                    onStopClick = { onStopClick() }
                )
                .noRippleClickable {
                    when {
                        exoPlayer.isPlaying -> {
                            showController()
                            exoPlayer.pausePlayer()
                        }

                        else -> {
                            hideControllersDelayed()
                            exoPlayer.startPlayer()
                        }
                    }
                },
            factory = { context ->
                PlayerView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(Color.BLACK)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    controllerAutoShow = false
                    useController = false
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowFastForwardButton(false)
                    setShowRewindButton(false)
                    player = exoPlayer
                }
            }
        )
        AnimatedVisibility(
            visible = shouldShowController,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(Modifier.fillMaxSize()) {
                PlayerPlayStateIcon(
                    modifier = Modifier.align(Alignment.Center),
                    playing = exoPlayer.isPlaying,
                )

                PlayerControllerView(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    title = title,
                    playingPosition = playingPosition,
                    playingDuration = exoPlayer.contentDuration,
                    onChangePosition = {
                        changePositionJob.cancel()
                        changePositionJob = coroutineScope.launch(SupervisorJob()) {
                            runCatching {
                                if (hideJobActive)
                                    hideControllersDelayed()
                                delay(500)
                                exoPlayer.seekTo(it)
                            }
                        }
                    }
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

private class SeekState {
    private var seekStartMs: Long = 0
    fun calcSeekMultiplier(longPress: Boolean): Float {
        if (!longPress) {
            seekStartMs = 0
            return 1f
        } else {
            if (seekStartMs == 0L)
                seekStartMs = SystemClock.uptimeMillis()
            return max((SystemClock.uptimeMillis() - seekStartMs) / 1000f, 1f)
        }
    }
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

@Composable
private fun PlayerControllerView(
    title: String,
    playingPosition: Long,
    playingDuration: Long,
    onChangePosition: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (playingDuration < 0f) return
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        val valueRange = remember(playingDuration) { 0f..playingDuration.toFloat() }
        var position by remember(playingPosition) {
            mutableFloatStateOf(playingPosition.toFloat())
        }
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = position,
            valueRange = valueRange,
            onValueChange = {
                position = it
                onChangePosition(it.toLong())
            }
        )
        Row {
            Spacer(modifier = Modifier.weight(1f))
            val positionText = remember(position, playingDuration) {
                "${formatDuration(position.toLong())} / ${formatDuration(playingDuration)}"
            }
            Text(
                text = positionText,
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun PlayerPlayStateIcon(
    playing: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(68.dp)
            .background(
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .padding(16.dp)
    ) {
        Icon(
            modifier = Modifier.fillMaxSize(),
            tint = LightColorScheme.background,
            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = "Play video icon"
        )
    }
}

private fun formatDuration(duration: Long): String {
    val hours = duration / 3600000
    val minutes = (duration % 3600000) / 60000
    val seconds = (duration % 60000) / 1000

    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

private var longPress = false
private fun Modifier.onPlayerKeyEvent(
    onUpClick: (longPress: Boolean) -> Unit,
    onPauseClick: (longPress: Boolean) -> Unit,
    onPlayClick: (longPress: Boolean) -> Unit,
    onPlayPauseClick: (longPress: Boolean) -> Unit,
    onLeftClick: (longPress: Boolean) -> Unit,
    onRightClick: (longPress: Boolean) -> Unit,
    onStopClick: (longPress: Boolean) -> Unit
): Modifier {
    return onKeyEvent { keyEvent ->
        if (!isOwnKeyCode(keyEvent)) return@onKeyEvent false
        if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
            try {
                when (keyEvent.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        onUpClick(longPress)
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                    KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> {
                        onLeftClick(longPress)
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                    KeyEvent.KEYCODE_MEDIA_NEXT,
                    KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> {
                        onRightClick(longPress)
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        onPlayPauseClick(longPress)
                        true
                    }

                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        onPlayClick(longPress)
                        true
                    }

                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        onPauseClick(longPress)
                        true
                    }

                    KeyEvent.KEYCODE_MEDIA_STOP -> {
                        onStopClick(longPress)
                        true
                    }

                    else -> {
                        false
                    }
                }
            } finally {
                longPress = true
            }
        } else {
            longPress = false
            true
        }
    }
}

@Composable
private fun ExoPlayer.observeLifeCycle() {
    LocalLifecycleOwner.current.lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    pause()
                }

                else -> Unit
            }
        }
    })
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
        KeyEvent.KEYCODE_DPAD_UP,
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
