package ru.slartus.boostbuddy.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import ru.slartus.boostbuddy.utils.Platform

private const val FOCUSED_SCALE = 1.03f

@Composable
internal fun isTvPlatform(): Boolean =
    LocalPlatformConfiguration.current.platform == Platform.AndroidTV

@Composable
internal fun Modifier.tvFocusHighlight(shape: Shape): Modifier {
    val isTv = isTvPlatform()
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isTv && isFocused) FOCUSED_SCALE else 1f)
    val borderColor = MaterialTheme.colorScheme.onSurface
    if (!isTv) return this
    return this
        .onFocusChanged { isFocused = it.hasFocus }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(if (isFocused) Modifier.border(3.dp, borderColor, shape) else Modifier)
}

@Composable
internal fun rememberTvInitialFocus(): FocusRequester {
    val requester = remember { FocusRequester() }
    val isTv = isTvPlatform()
    LaunchedEffect(requester, isTv) {
        if (isTv) requester.requestFocus()
    }
    return requester
}
