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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration
import ru.slartus.boostbuddy.utils.Platform
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.ui.common.noRippleClickable
import ru.slartus.boostbuddy.ui.theme.LightColorScheme
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

private val HIDE_CONTROLLER_DELAY = 5.seconds
private val SEEK_INCREMENT = 5.seconds
private val DOUBLE_TAP_SEEK = 10.seconds
private val SEEK_FEEDBACK_TIMEOUT = 1.seconds

// Pinch-in трактуем как «жест выхода» — любое уменьшение zoom относительно
// начала pinch-сессии возвращает видео в дефолт (1f без смещения). Это даёт
// предсказуемый способ выйти из любого «застрявшего» масштабированного/
// смещённого состояния, особенно на эмуляторе, где Ctrl+drag даёт дёрганные
// pointer-события и pinch-in редко доводит zoom до 1f сам по себе.
// Если хочется тонкая подстройка zoom — это pinch-out от 1f, не in.
private const val ZOOM_SNAP_THRESHOLD = 1.05f
private const val PINCH_DECREASE_NOISE = 0.05f
private const val ZOOM_MAX = 5f
private const val ZOOM_BADGE_AUTOHIDE_MS = 1500L
private const val LIVE_EDGE_THRESHOLD_MS = 5_000L
private const val LIVE_EDGE_POLL_INTERVAL_MS = 1_000L
private val LIVE_RED = ComposeColor(0xFFE53935)

private fun Float.isZoomed(): Boolean = this > 1f

private enum class SeekDirection { FORWARD, BACKWARD }

private data class SeekFeedback(val direction: SeekDirection, val seconds: Int)

private class JobHolder {
    var job: Job? = null
}

@OptIn(UnstableApi::class)
@Composable
internal fun VideoPlayerChrome(
    exoPlayer: ExoPlayer,
    title: String,
    playingPosition: Long,
    isEnded: Boolean,
    isLive: Boolean,
    onLiveEdgeChanged: (Boolean) -> Unit,
    onStopClick: () -> Unit,
    onSettingsClick: (() -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val controllerState = rememberVideoControllerState()
    val focusRequester = remember { FocusRequester() }
    val isAtv = LocalPlatformConfiguration.current.platform == Platform.AndroidTV
    var playerSize by remember { mutableStateOf(IntSize.Zero) }

    var changePositionJob by remember { mutableStateOf<Job?>(null) }
    val seekState = remember { SeekState() }

    var zoom by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val resetZoom = remember {
        {
            zoom = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }

    var zoomBadgeVisible by remember { mutableStateOf(false) }
    var isInitialZoom by remember { mutableStateOf(true) }
    LaunchedEffect(zoom) {
        // Первая композиция стартует с zoom=1f — без визуального шума пропускаем.
        if (isInitialZoom) {
            isInitialZoom = false
            return@LaunchedEffect
        }
        // После снапа к 1f бейдж не должен мигать «x1.0» — сразу прячем.
        if (!zoom.isZoomed()) {
            zoomBadgeVisible = false
            return@LaunchedEffect
        }
        zoomBadgeVisible = true
        delay(ZOOM_BADGE_AUTOHIDE_MS)
        zoomBadgeVisible = false
    }

    var seekFeedback by remember { mutableStateOf<SeekFeedback?>(null) }
    val seekFeedbackJobHolder = remember { JobHolder() }

    var isAtLiveEdge by remember { mutableStateOf(true) }
    val currentOnLiveEdgeChanged by rememberUpdatedState(onLiveEdgeChanged)
    LaunchedEffect(isLive, exoPlayer) {
        if (!isLive) return@LaunchedEffect
        while (isActive) {
            val offset = exoPlayer.currentLiveOffset
            val newValue = offset == C.TIME_UNSET || offset < LIVE_EDGE_THRESHOLD_MS
            if (newValue != isAtLiveEdge) {
                isAtLiveEdge = newValue
                currentOnLiveEdgeChanged(newValue)
            }
            delay(LIVE_EDGE_POLL_INTERVAL_MS)
        }
    }

    val applySeekTick by rememberUpdatedState<(Offset) -> Unit> { offset ->
        val width = playerSize.width
        if (width <= 0) return@rememberUpdatedState
        val direction = if (offset.x > width / 2f) {
            SeekDirection.FORWARD
        } else {
            SeekDirection.BACKWARD
        }
        val previous = seekFeedback
        // Пока активна серия в одну сторону — игнорируем тапы в противоположную половину,
        // чтобы случайный/промазанный тап не разворачивал перемотку назад.
        if (previous != null && previous.direction != direction) {
            return@rememberUpdatedState
        }
        val seekMs = DOUBLE_TAP_SEEK.toLong(DurationUnit.MILLISECONDS)
        val newSeconds = if (previous?.direction == direction) {
            previous.seconds + DOUBLE_TAP_SEEK.inWholeSeconds.toInt()
        } else {
            DOUBLE_TAP_SEEK.inWholeSeconds.toInt()
        }
        seekFeedback = SeekFeedback(direction, newSeconds)
        val target = when (direction) {
            SeekDirection.FORWARD -> exoPlayer.currentPosition + seekMs
            SeekDirection.BACKWARD -> exoPlayer.currentPosition - seekMs
        }
        exoPlayer.seekTo(target.coerceAtLeast(0L))
        controllerState.hide()

        seekFeedbackJobHolder.job?.cancel()
        seekFeedbackJobHolder.job = coroutineScope.launch {
            delay(SEEK_FEEDBACK_TIMEOUT)
            seekFeedback = null
        }
    }

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
                    onStopClick = { onStopClick() },
                    onSettingsClick = onSettingsClick?.let { { _: Boolean -> it() } },
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
                                // detectTransformGestures триггерится и на одиночных
                                // касаниях/фантомных pointer-событиях (например при
                                // закрытии ModalBottomSheet со слайдером скорости):
                                // видео тихо «раздувается» и сдвигается. Поэтому
                                // считаем пальцы вручную:
                                //  • zoom == 1f и < 2 пальцев — игнорим (фантомы и
                                //    случайные свайпы не должны масштабировать видео);
                                //  • 2+ пальцев — полноценный pinch + pan;
                                //  • zoom > 1f и 1 палец — pan уже зумнутого видео.
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    var pinchStartZoom: Float? = null
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.changes.none { it.pressed }) break
                                        val pressedCount = event.changes.count { it.pressed }
                                        // Pinch уже начался (≥2 пальцев) и один поднялся —
                                        // завершаем жест целиком. Иначе оставшийся палец
                                        // интерпретируется как single-finger pan, zoom
                                        // фиксируется и pinch-in уже не доводит его до 1f.
                                        if (pinchStartZoom != null && pressedCount < 2) break
                                        if (pressedCount >= 2 && pinchStartZoom == null) {
                                            pinchStartZoom = zoom
                                        }
                                        if (pressedCount < 2 && !zoom.isZoomed()) continue
                                        if (!event.changes.fastAny { it.positionChanged() }) continue
                                        val zoomChange =
                                            if (pressedCount >= 2) event.calculateZoom() else 1f
                                        val panChange = event.calculatePan()
                                        val newZoom = (zoom * zoomChange).coerceIn(1f, ZOOM_MAX)
                                        if (newZoom.isZoomed()) {
                                            val maxOffsetX =
                                                (playerSize.width * (newZoom - 1f)) / 2f
                                            val maxOffsetY =
                                                (playerSize.height * (newZoom - 1f)) / 2f
                                            offsetX = (offsetX + panChange.x)
                                                .coerceIn(-maxOffsetX, maxOffsetX)
                                            offsetY = (offsetY + panChange.y)
                                                .coerceIn(-maxOffsetY, maxOffsetY)
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                        zoom = newZoom
                                        event.changes.fastForEach { it.consume() }
                                    }
                                    val startZoom = pinchStartZoom
                                    val zoomDecreased =
                                        startZoom != null && zoom < startZoom - PINCH_DECREASE_NOISE
                                    if (zoom < ZOOM_SNAP_THRESHOLD || zoomDecreased) {
                                        resetZoom()
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        if (seekFeedback != null) {
                                            applySeekTick(offset)
                                        } else if (controllerState.isVisible) {
                                            controllerState.hide()
                                        } else {
                                            controllerState.showWithAutoHide()
                                        }
                                    },
                                    onDoubleTap = { offset ->
                                        // Любое нестандартное состояние (зум или
                                        // случайно накопленный pan по уже зумнутому,
                                        // потом сброшенному видео) — двойной тап
                                        // сбрасывает. Дефолт — seek по половине.
                                        if (zoom.isZoomed() || offsetX != 0f || offsetY != 0f) {
                                            resetZoom()
                                        } else {
                                            applySeekTick(offset)
                                        }
                                    },
                                )
                            }
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                translationX = offsetX
                                translationY = offsetY
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
            visible = seekFeedback != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            seekFeedback?.let { feedback ->
                SeekFeedbackOverlay(feedback)
            }
        }

        AnimatedVisibility(
            visible = controllerState.isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (zoom.isZoomed()) {
                        ZoomChip(
                            zoom = zoom,
                            onClick = {
                                controllerState.showWithAutoHide()
                                resetZoom()
                            },
                        )
                    }
                    if (onSettingsClick != null) {
                        PlayerQualityButton(
                            onClick = {
                                controllerState.showWithAutoHide()
                                onSettingsClick()
                            },
                        )
                    }
                }

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

        // Бейдж рисуется ПОСЛЕ controllerState, иначе fillMaxSize-overlay
        // контроллера перехватывает клики по нему.
        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            visible = zoomBadgeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ZoomBadge(zoom = zoom, onClick = resetZoom)
        }

        if (isLive) {
            LiveBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                isAtLiveEdge = isAtLiveEdge,
                onGoLiveClick = { exoPlayer.seekToDefaultPosition() },
            )
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
private fun SeekFeedbackOverlay(
    feedback: SeekFeedback,
    modifier: Modifier = Modifier,
) {
    val isForward = feedback.direction == SeekDirection.FORWARD
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(if (isForward) Alignment.CenterEnd else Alignment.CenterStart)
                .background(
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(24.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = if (isForward) {
                        Icons.Filled.FastForward
                    } else {
                        Icons.Filled.FastRewind
                    },
                    contentDescription = null,
                    tint = LightColorScheme.background,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${feedback.seconds} сек",
                    color = LightColorScheme.background,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
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

@Composable
private fun LiveBadge(
    isAtLiveEdge: Boolean,
    onGoLiveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (isAtLiveEdge) {
        LIVE_RED
    } else {
        MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)
    }
    val contentColor = if (isAtLiveEdge) {
        ComposeColor.White
    } else {
        ComposeColor.White.copy(alpha = 0.85f)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .clickable(enabled = !isAtLiveEdge, onClick = onGoLiveClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "LIVE",
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ZoomBadge(
    zoom: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .background(
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                shape = CircleShape,
            )
            .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = formatZoom(zoom),
            color = LightColorScheme.background,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ZoomChip(
    zoom: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = formatZoom(zoom),
            color = LightColorScheme.background,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun formatZoom(zoom: Float): String {
    val tenths = (zoom * 10f).roundToInt()
    val whole = tenths / 10
    val rem = tenths % 10
    return "x$whole.$rem"
}

@Composable
private fun PlayerQualityButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Box(
        modifier = modifier
            .size(48.dp)
            .background(
                color = MaterialTheme.colorScheme.scrim.copy(
                    alpha = if (isFocused) 0.85f else 0.5f
                ),
                shape = CircleShape,
            )
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(10.dp),
    ) {
        Icon(
            modifier = Modifier.fillMaxSize(),
            tint = LightColorScheme.background,
            imageVector = Icons.Filled.Settings,
            contentDescription = "Качество видео",
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
    onStopClick: (longPress: Boolean) -> Unit,
    onSettingsClick: ((longPress: Boolean) -> Unit)? = null,
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

                    KeyEvent.KEYCODE_SETTINGS,
                    KeyEvent.KEYCODE_INFO,
                    KeyEvent.KEYCODE_MENU -> {
                        onSettingsClick?.invoke(longPress)
                        onSettingsClick != null
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
        KeyEvent.KEYCODE_MEDIA_PAUSE,
        KeyEvent.KEYCODE_SETTINGS,
        KeyEvent.KEYCODE_INFO,
        KeyEvent.KEYCODE_MENU -> true

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
