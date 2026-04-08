package ru.slartus.boostbuddy.ui.widgets

import android.graphics.Color
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration
import ru.slartus.boostbuddy.utils.Platform
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.ui.common.noRippleClickable
import ru.slartus.boostbuddy.ui.theme.LightColorScheme
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

private val HIDE_CONTROLLER_DELAY = 5.seconds
private val SEEK_INCREMENT = 5.seconds
private val DOUBLE_TAP_SEEK = 10.seconds

@OptIn(UnstableApi::class)
@Composable
internal fun VideoPlayerChrome(
    exoPlayer: ExoPlayer,
    title: String,
    playingPosition: Long,
    isEnded: Boolean,
    onStopClick: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val controllerState = rememberVideoControllerState()
    val focusRequester = remember { FocusRequester() }
    val isAtv = LocalPlatformConfiguration.current.platform == Platform.AndroidTV
    var playerSize by remember { mutableStateOf(IntSize.Zero) }

    var changePositionJob by remember { mutableStateOf<Job?>(null) }
    val seekState = remember { SeekState() }

    LaunchedEffect(isEnded) {
        if (isEnded) {
            controllerState.show()
        }
    }
    Box {
        AndroidView(
            modifier = Modifier
                .focusable()
                .focusRequester(focusRequester)
                .onPlayerKeyEvent(
                    onUpClick = {
                        controllerState.showWithAutoHide()
                    },
                    onPauseClick = {
                        controllerState.show()
                        exoPlayer.pausePlayer()
                    },
                    onPlayClick = {
                        controllerState.showWithAutoHide()
                        exoPlayer.startPlayer()
                    },
                    onPlayPauseClick = {
                        if (exoPlayer.isPlaying) {
                            controllerState.show()
                            exoPlayer.pausePlayer()
                        } else {
                            controllerState.showWithAutoHide()
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
                        controllerState.showWithAutoHide()
                    },
                    onRightClick = { longPress ->
                        val seekMultiplier = seekState.calcSeekMultiplier(longPress)
                        exoPlayer.seekTo(
                            exoPlayer.currentPosition + (SEEK_INCREMENT.toLong(
                                DurationUnit.MILLISECONDS
                            ) * seekMultiplier).toLong()
                        )
                        controllerState.showWithAutoHide()
                    },
                    onStopClick = { onStopClick() }
                )
                .then(
                    if (isAtv) {
                        Modifier.noRippleClickable {
                            if (exoPlayer.isPlaying) {
                                controllerState.show()
                                exoPlayer.pausePlayer()
                            } else {
                                controllerState.showWithAutoHide()
                                exoPlayer.startPlayer()
                            }
                        }
                    } else {
                        Modifier
                            .onSizeChanged { playerSize = it }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        if (controllerState.isVisible) {
                                            controllerState.hide()
                                        } else {
                                            controllerState.showWithAutoHide()
                                        }
                                    },
                                    onDoubleTap = { offset ->
                                        val seekMs = DOUBLE_TAP_SEEK
                                            .toLong(DurationUnit.MILLISECONDS)
                                        val width = playerSize.width
                                        val target = if (width > 0 && offset.x > width / 2f) {
                                            exoPlayer.currentPosition + seekMs
                                        } else {
                                            exoPlayer.currentPosition - seekMs
                                        }
                                        exoPlayer.seekTo(target.coerceAtLeast(0L))
                                        controllerState.hide()
                                    }
                                )
                            }
                    }
                ),
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
            visible = controllerState.isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(Modifier.fillMaxSize()) {
                PlayerPlayStateIcon(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .then(
                            if (!isAtv) {
                                Modifier.noRippleClickable {
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pausePlayer()
                                        controllerState.show()
                                    } else {
                                        exoPlayer.startPlayer()
                                        controllerState.showWithAutoHide()
                                    }
                                }
                            } else {
                                Modifier
                            }
                        ),
                    playing = exoPlayer.isPlaying,
                )

                PlayerControllerView(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    title = title,
                    playingPosition = playingPosition,
                    playingDuration = exoPlayer.contentDuration,
                    onChangePosition = { newPosition ->
                        changePositionJob?.cancel()
                        val autoHideWasActive = controllerState.isAutoHideActive

                        changePositionJob = coroutineScope.launch(SupervisorJob()) {
                            runCatching {
                                if (autoHideWasActive) {
                                    controllerState.scheduleAutoHide()
                                }
                                delay(500)
                                exoPlayer.seekTo(newPosition)
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

@Composable
private fun PlayerControllerView(
    title: String,
    playingPosition: Long,
    playingDuration: Long,
    onChangePosition: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (playingDuration <= 0L) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))

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

private fun Modifier.onPlayerKeyEvent(
    onUpClick: (longPress: Boolean) -> Unit,
    onPauseClick: (longPress: Boolean) -> Unit,
    onPlayClick: (longPress: Boolean) -> Unit,
    onPlayPauseClick: (longPress: Boolean) -> Unit,
    onLeftClick: (longPress: Boolean) -> Unit,
    onRightClick: (longPress: Boolean) -> Unit,
    onStopClick: (longPress: Boolean) -> Unit
): Modifier = composed {
    var longPress by remember { mutableStateOf(false) }

    onKeyEvent { keyEvent ->
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

                    else -> false
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

        else -> false
    }

@Stable
private class VideoControllerState(
    private val scope: CoroutineScope
) {
    var isVisible by mutableStateOf(false)
        private set

    private var hideJob: Job? = null

    val isAutoHideActive: Boolean
        get() = hideJob?.isActive == true

    fun show() {
        hideJob?.cancel()
        isVisible = true
    }

    fun hide() {
        hideJob?.cancel()
        isVisible = false
    }

    fun showWithAutoHide() {
        show()
        scheduleAutoHide()
    }

    fun scheduleAutoHide() {
        hideJob?.cancel()
        hideJob = scope.launch(SupervisorJob()) {
            runCatching {
                delay(HIDE_CONTROLLER_DELAY)
                isVisible = false
            }
        }
    }
}

@Composable
private fun rememberVideoControllerState(): VideoControllerState {
    val scope = rememberCoroutineScope()
    return remember(scope) {
        VideoControllerState(scope)
    }
}
