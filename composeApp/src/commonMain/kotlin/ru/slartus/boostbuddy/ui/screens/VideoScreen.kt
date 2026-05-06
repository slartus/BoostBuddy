package ru.slartus.boostbuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.slartus.boostbuddy.components.blog.text
import ru.slartus.boostbuddy.components.video.VideoComponent
import ru.slartus.boostbuddy.components.video.downloadableOptions
import ru.slartus.boostbuddy.components.video.timeCodeMs
import ru.slartus.boostbuddy.components.video.usableOptions
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.VideoQuality
import ru.slartus.boostbuddy.ui.common.HideSystemBarsEffect
import ru.slartus.boostbuddy.ui.common.KeepScreenOnEffect
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration
import ru.slartus.boostbuddy.ui.widgets.VideoPlayer
import ru.slartus.boostbuddy.utils.Platform
import kotlin.math.roundToInt

private val PLAYBACK_SPEED_PRESETS: List<Float> = listOf(
    0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 3f
)
private const val PLAYBACK_SPEED_MIN = 0.5f
private const val PLAYBACK_SPEED_MAX = 3f
private const val PLAYBACK_SPEED_STEP = 0.05f
private const val PLAYBACK_SPEED_EPSILON = 0.005f

private fun Float.matchesSpeed(other: Float): Boolean = abs(this - other) < PLAYBACK_SPEED_EPSILON

@Composable
internal fun VideoScreen(component: VideoComponent) {
    val state by component.viewStates.subscribeAsState()

    KeepScreenOnEffect()
    HideSystemBarsEffect()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        state.postData?.let { postData ->
            val qualityOptions = remember(postData) { postData.playerUrls.usableOptions() }
            val downloadOptions = remember(postData) { postData.playerUrls.downloadableOptions() }
            VideoPlayer(
                vid = postData.vid,
                playerUrl = state.playerUrl,
                title = postData.title,
                position = postData.timeCodeMs,
                playbackSpeed = state.playbackSpeed,
                isLive = state.isLive,
                isAtLiveEdge = state.isAtLiveEdge,
                retryToken = state.retryToken,
                onVideoStateChange = component::onVideoStateChanged,
                onContentPositionChange = component::onContentPositionChange,
                onLiveEdgeChanged = component::onLiveEdgeChanged,
                onStopClick = component::onStopClicked,
                onSettingsClick = component::onSettingsClicked,
            )

            if (state.settingsSheetVisible) {
                PlayerSettingsSheet(
                    qualities = qualityOptions,
                    downloadOptions = if (state.isLive) emptyList() else downloadOptions,
                    currentQuality = state.playerUrl.quality,
                    currentSpeed = state.playbackSpeed,
                    speedAvailable = !state.isLive || !state.isAtLiveEdge,
                    onQualitySelected = { component.onQualityItemClicked(it) },
                    onSpeedSelected = { component.onPlaybackSpeedSelected(it) },
                    onDownloadSelected = { component.onDownloadQualitySelected(it) },
                    onDismiss = { component.onSettingsSheetDismissed() },
                )
            }
        }

        if (state.loading && !state.streamEnded && !state.playbackError) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(58.dp)
            )
        }

        if (state.streamEnded) {
            StreamEndedOverlay(
                modifier = Modifier.fillMaxSize(),
                onCloseClick = component::onStopClicked,
            )
        }

        if (state.playbackError) {
            PlaybackErrorOverlay(
                modifier = Modifier.fillMaxSize(),
                onRetryClick = component::onRetryClicked,
                onCloseClick = component::onStopClicked,
            )
        }
    }
}

@Composable
private fun StreamEndedOverlay(
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Стрим завершён",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Button(onClick = onCloseClick) {
                Text(text = "Закрыть")
            }
        }
    }
}

@Composable
private fun PlaybackErrorOverlay(
    onRetryClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Не удалось воспроизвести видео",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRetryClick) {
                    Text(text = "Повторить")
                }
                Button(onClick = onCloseClick) {
                    Text(text = "Закрыть")
                }
            }
        }
    }
}

private enum class SettingsSection { Root, Quality, Speed, Download }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSettingsSheet(
    qualities: List<PlayerUrl>,
    downloadOptions: List<PlayerUrl>,
    currentQuality: VideoQuality,
    currentSpeed: Float,
    speedAvailable: Boolean,
    onQualitySelected: (PlayerUrl) -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onDownloadSelected: (PlayerUrl) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var section by remember { mutableStateOf(SettingsSection.Root) }

    ModalBottomSheet(
        modifier = modifier.navigationBarsPadding(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSheetHeader(
                section = section,
                onBack = { section = SettingsSection.Root },
            )
            when (section) {
                SettingsSection.Root -> SettingsRootPanel(
                    qualityLabel = currentQuality.text,
                    speedLabel = formatSpeed(currentSpeed),
                    downloadAvailable = downloadOptions.isNotEmpty(),
                    speedAvailable = speedAvailable,
                    onQualityClick = { section = SettingsSection.Quality },
                    onSpeedClick = { section = SettingsSection.Speed },
                    onDownloadClick = { section = SettingsSection.Download },
                )

                SettingsSection.Quality -> QualityPanel(
                    qualities = qualities,
                    currentQuality = currentQuality,
                    onSelected = onQualitySelected,
                )

                SettingsSection.Speed -> SpeedPanel(
                    currentSpeed = currentSpeed,
                    onSelected = onSpeedSelected,
                )

                SettingsSection.Download -> DownloadPanel(
                    qualities = downloadOptions,
                    onSelected = onDownloadSelected,
                )
            }
        }
    }
}

@Composable
private fun SettingsSheetHeader(
    section: SettingsSection,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (section != SettingsSection.Root) {
            IconButton(
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = onBack,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                )
            }
        }
        Text(
            text = when (section) {
                SettingsSection.Root -> "Настройки воспроизведения"
                SettingsSection.Quality -> "Качество"
                SettingsSection.Speed -> "Скорость воспроизведения"
                SettingsSection.Download -> "Скачать видео"
            },
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun SettingsRootPanel(
    qualityLabel: String,
    speedLabel: String,
    downloadAvailable: Boolean,
    speedAvailable: Boolean,
    onQualityClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsRootRow(
            title = "Качество",
            value = qualityLabel,
            onClick = onQualityClick,
        )
        if (speedAvailable) {
            SettingsRootRow(
                title = "Скорость воспроизведения",
                value = speedLabel,
                onClick = onSpeedClick,
            )
        }
        if (downloadAvailable) {
            SettingsRootRow(
                title = "Скачать видео",
                value = "",
                onClick = onDownloadClick,
            )
        }
    }
}

@Composable
private fun SettingsRootRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
        )
    }
}

@Composable
private fun QualityPanel(
    qualities: List<PlayerUrl>,
    currentQuality: VideoQuality,
    onSelected: (PlayerUrl) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        qualities.forEach { item ->
            CheckmarkRow(
                title = item.quality.text,
                checked = item.quality == currentQuality,
                onClick = { onSelected(item) },
            )
        }
    }
}

@Composable
private fun DownloadPanel(
    qualities: List<PlayerUrl>,
    onSelected: (PlayerUrl) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        qualities.forEach { item ->
            CheckmarkRow(
                title = item.quality.text,
                checked = false,
                onClick = { onSelected(item) },
            )
        }
    }
}

@Composable
private fun SpeedPanel(
    currentSpeed: Float,
    onSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAtv = LocalPlatformConfiguration.current.platform == Platform.AndroidTV
    if (isAtv) {
        SpeedPanelTv(
            currentSpeed = currentSpeed,
            onSelected = onSelected,
            modifier = modifier,
        )
    } else {
        SpeedPanelMobile(
            currentSpeed = currentSpeed,
            onSelected = onSelected,
            modifier = modifier,
        )
    }
}

@Composable
private fun SpeedPanelTv(
    currentSpeed: Float,
    onSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PLAYBACK_SPEED_PRESETS.forEach { preset ->
            CheckmarkRow(
                title = formatSpeed(preset),
                checked = preset.matchesSpeed(currentSpeed),
                onClick = { onSelected(preset) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeedPanelMobile(
    currentSpeed: Float,
    onSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Локальный буфер во время drag слайдера: чтобы не писать в Settings и не дёргать ExoPlayer
    // на каждое микро-движение пальца. Запись — только на отпускании или явном клике.
    var dragValue by remember(currentSpeed) { mutableFloatStateOf(currentSpeed) }
    val displaySpeed = dragValue.coerceIn(PLAYBACK_SPEED_MIN, PLAYBACK_SPEED_MAX)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = formatSpeed(displaySpeed),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    val next = roundSpeed(
                        (displaySpeed - PLAYBACK_SPEED_STEP).coerceAtLeast(PLAYBACK_SPEED_MIN)
                    )
                    dragValue = next
                    onSelected(next)
                },
                enabled = displaySpeed > PLAYBACK_SPEED_MIN,
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Уменьшить скорость")
            }
            Slider(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                value = displaySpeed,
                valueRange = PLAYBACK_SPEED_MIN..PLAYBACK_SPEED_MAX,
                onValueChange = { dragValue = it },
                onValueChangeFinished = { onSelected(roundSpeed(dragValue)) },
            )
            IconButton(
                onClick = {
                    val next = roundSpeed(
                        (displaySpeed + PLAYBACK_SPEED_STEP).coerceAtMost(PLAYBACK_SPEED_MAX)
                    )
                    dragValue = next
                    onSelected(next)
                },
                enabled = displaySpeed < PLAYBACK_SPEED_MAX,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Увеличить скорость")
            }
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PLAYBACK_SPEED_PRESETS.forEach { preset ->
                val selected = preset.matchesSpeed(currentSpeed)
                if (selected) {
                    FilterChip(
                        selected = true,
                        onClick = { onSelected(preset) },
                        label = { Text(formatSpeed(preset)) },
                    )
                } else {
                    AssistChip(
                        onClick = { onSelected(preset) },
                        label = { Text(formatSpeed(preset)) },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CheckmarkRow(
    title: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (checked) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null)
        } else {
            Box(Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(text = title)
    }
}

private fun formatSpeed(speed: Float): String {
    val rounded = roundSpeed(speed)
    val text = if (rounded == rounded.toInt().toFloat()) {
        rounded.toInt().toString()
    } else {
        val cents = (rounded * 100).roundToInt()
        if (cents % 10 == 0) {
            "${cents / 100}.${(cents / 10) % 10}"
        } else {
            "${cents / 100}.${((cents / 10) % 10)}${cents % 10}"
        }
    }
    return "${text}x"
}

private fun roundSpeed(speed: Float): Float =
    ((speed * 100).roundToInt() / 100f)
