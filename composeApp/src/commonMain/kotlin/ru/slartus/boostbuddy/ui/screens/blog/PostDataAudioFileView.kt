package ru.slartus.boostbuddy.ui.screens.blog

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.slartus.boostbuddy.data.log.logger
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.linkColor
import ru.slartus.boostbuddy.ui.common.HorizontalSpacer
import ru.slartus.boostbuddy.ui.common.VerticalSpacer
import ru.slartus.boostbuddy.utils.AudioPlayerState
import ru.slartus.boostbuddy.utils.AudioPlayerStateListener
import ru.slartus.boostbuddy.utils.rememberAudioPlayer


@Composable
internal fun PostDataAudioFileView(
    signedQuery: String,
    postData: Content.AudioFile
) {
    var playerState by remember { mutableStateOf<AudioPlayerState>(AudioPlayerState.Idle) }
    var position by remember { mutableStateOf(0f) }
    val listener: AudioPlayerStateListener = remember {
        object : AudioPlayerStateListener {
            override fun onStateChanged(state: AudioPlayerState) {
                playerState = state
                when (state) {
                    AudioPlayerState.Completed,
                    AudioPlayerState.Error,
                    AudioPlayerState.Idle,
                    AudioPlayerState.Prepared,
                    AudioPlayerState.Preparing,
                    AudioPlayerState.Starting,
                    AudioPlayerState.Stopped -> Unit

                    is AudioPlayerState.Paused -> position = state.position.toFloat()
                    is AudioPlayerState.Playing -> position = state.position.toFloat()

                }
            }
        }
    }
    val audioPlayer = rememberAudioPlayer(listener)
    Column(
        Modifier
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.secondary,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        FocusableRow(verticalAlignment = CenterVertically) {
            Icon(
                modifier = Modifier.size(28.dp),
                tint = linkColor,
                imageVector = Icons.Filled.Audiotrack,
                contentDescription = "Play video icon"
            )
            HorizontalSpacer(4.dp)
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = postData.title,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )
        }
        VerticalSpacer(4.dp)
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                runCatching {
                    when (playerState) {
                        AudioPlayerState.Idle,
                        AudioPlayerState.Completed,
                        AudioPlayerState.Stopped ->
                            audioPlayer.play(postData.url + signedQuery)

                        AudioPlayerState.Error -> if (position > 0f) audioPlayer.resume()
                        else audioPlayer.play(postData.url + signedQuery)

                        is AudioPlayerState.Paused -> audioPlayer.resume()

                        AudioPlayerState.Prepared,
                        AudioPlayerState.Preparing,
                        AudioPlayerState.Starting -> Unit

                        is AudioPlayerState.Playing -> audioPlayer.pause()

                    }
                }.onFailure {
                    logger.e("audioPlayer.play", it)
                }
            },
            verticalAlignment = CenterVertically
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Center
            ) {
                if (playerState is AudioPlayerState.Preparing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = linkColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        tint = linkColor,
                        imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play audio icon"
                    )
                }
            }
            HorizontalSpacer(4.dp)
            Text(
                text = "${formatDuration(position.toInt())} / ${formatDuration(postData.duration.toInt())}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )
            HorizontalSpacer(4.dp)
            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = position,
                valueRange = 0f..postData.duration.toFloat(),
                onValueChange = {
                    if (playerState.isPlaying || playerState is AudioPlayerState.Paused)
                        audioPlayer.seekTo(it.toInt())
                }
            )
        }
    }
}

private fun formatDuration(duration: Int): String {
    val hours = duration / 3600
    val minutes = (duration % 3600) / 60
    val seconds = duration % 60

    return if (hours > 0) "${hours.toTime()}:${minutes.toTime()}:${seconds.toTime()}"
    else "${minutes.toTime()}:${seconds.toTime()}"
}

private fun Int.toTime(): String = toString().padStart(2, '0')

private val AudioPlayerState.isPlaying
    get() = when (this) {
        is AudioPlayerState.Playing -> true
        else -> false
    }