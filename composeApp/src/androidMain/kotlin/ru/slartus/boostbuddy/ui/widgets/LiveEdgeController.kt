package ru.slartus.boostbuddy.ui.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val LIVE_EDGE_THRESHOLD_MS = 5_000L
private const val LIVE_EDGE_POLL_INTERVAL_MS = 1_000L
private val LIVE_RED = Color(0xFFE53935)

/**
 * Полностью отвечает за live-edge: поллит `currentLiveOffset`, дёргает callback'ом
 * `onLiveEdgeChanged` при пересечении порога, рисует LIVE-бейдж и обрабатывает
 * клик «go-live». Никакая другая часть плеера про live edge знать не должна.
 */
@Composable
internal fun LiveEdgeController(
    exoPlayer: ExoPlayer,
    isAtLiveEdge: Boolean,
    isControllerVisible: Boolean,
    onLiveEdgeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnLiveEdgeChanged by rememberUpdatedState(onLiveEdgeChanged)
    val currentIsAtLiveEdge by rememberUpdatedState(isAtLiveEdge)
    LaunchedEffect(exoPlayer) {
        var lastReported = currentIsAtLiveEdge
        while (isActive) {
            val offset = exoPlayer.currentLiveOffset
            if (offset != C.TIME_UNSET) {
                val newValue = offset < LIVE_EDGE_THRESHOLD_MS
                if (newValue != lastReported) {
                    lastReported = newValue
                    currentOnLiveEdgeChanged(newValue)
                }
            }
            delay(LIVE_EDGE_POLL_INTERVAL_MS)
        }
    }

    AnimatedVisibility(
        modifier = modifier,
        visible = isControllerVisible || !isAtLiveEdge,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        LiveBadge(
            isAtLiveEdge = isAtLiveEdge,
            onGoLiveClick = { exoPlayer.seekToDefaultPosition() },
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
        Color.White
    } else {
        Color.White.copy(alpha = 0.85f)
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
