package ru.slartus.boostbuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.slartus.boostbuddy.components.blog.text
import ru.slartus.boostbuddy.components.video.VideoComponent
import ru.slartus.boostbuddy.components.video.timeCodeMs
import ru.slartus.boostbuddy.components.video.usableOptions
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.VideoQuality
import ru.slartus.boostbuddy.ui.common.HideSystemBarsEffect
import ru.slartus.boostbuddy.ui.common.KeepScreenOnEffect
import ru.slartus.boostbuddy.ui.widgets.VideoPlayer

@Composable
internal fun VideoScreen(component: VideoComponent) {
    val state by component.viewStates.subscribeAsState()

    KeepScreenOnEffect()
    HideSystemBarsEffect()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        state.postData?.let { postData ->
            val qualityOptions = remember(postData) { postData.playerUrls.usableOptions() }
            val hasMultipleQualities = qualityOptions.size > 1
            val onChangeQualityClick: (() -> Unit)? = remember(component, hasMultipleQualities) {
                if (hasMultipleQualities) {
                    { component.onChangeQualityClicked() }
                } else {
                    null
                }
            }
            VideoPlayer(
                vid = postData.vid,
                playerUrl = state.playerUrl,
                title = postData.title,
                position = postData.timeCodeMs,
                onVideoStateChange = { videoState ->
                    component.onVideoStateChanged(videoState)
                },
                onContentPositionChange = { component.onContentPositionChange(it) },
                onStopClick = { component.onStopClicked() },
                onChangeQualityClick = onChangeQualityClick,
            )

            if (state.qualitySheetVisible) {
                QualitySelectionSheet(
                    qualities = qualityOptions,
                    currentQuality = state.playerUrl.quality,
                    onSelected = { component.onQualityItemClicked(it) },
                    onDismiss = { component.onQualitySheetDismissed() },
                )
            }
        }

        if (state.loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(58.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualitySelectionSheet(
    qualities: List<PlayerUrl>,
    currentQuality: VideoQuality,
    onSelected: (PlayerUrl) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        modifier = modifier.navigationBarsPadding(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            qualities.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { onSelected(item) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.quality == currentQuality) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                        )
                    } else {
                        Box(Modifier.size(24.dp))
                    }
                    Box(Modifier.width(16.dp))
                    Text(text = item.quality.text)
                }
            }
        }
    }
}