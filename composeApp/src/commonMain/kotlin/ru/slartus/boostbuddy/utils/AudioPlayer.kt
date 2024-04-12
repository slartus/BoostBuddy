package ru.slartus.boostbuddy.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration

expect class AudioPlayer(
    platformConfiguration: PlatformConfiguration,
    coroutineScope: CoroutineScope,
    listener: AudioPlayerStateListener
) {
    fun play(url: String)
    fun pause()
    fun resume()
    fun release()
    fun seekTo(position: Int)
}

interface AudioPlayerStateListener {
    fun onStateChanged(state: AudioPlayerState)
}

sealed class AudioPlayerState {
    data object Idle : AudioPlayerState()
    data object Prepared : AudioPlayerState()
    data object Preparing : AudioPlayerState()
    data object Starting : AudioPlayerState()
    data class Playing(val position: Int) : AudioPlayerState()
    data class Paused(val position: Int) : AudioPlayerState()
    data object Stopped : AudioPlayerState()
    data object Completed : AudioPlayerState()
    data object Error : AudioPlayerState()
}

@Composable
internal fun rememberAudioPlayer(listener: AudioPlayerStateListener): AudioPlayer {
    val platformConfiguration = LocalPlatformConfiguration.current
    val coroutineScope = rememberCoroutineScope()
    val player = remember {
        AudioPlayer(platformConfiguration, coroutineScope, listener)
    }
    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }
    return player
}