package ru.slartus.boostbuddy.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration

expect class AudioPlayer(platformConfiguration: PlatformConfiguration) {
    fun play(url: String)
    fun pause()
    fun resume()
    fun release()
}


@Composable
internal fun rememberAudioPlayer(): AudioPlayer {
    val platformConfiguration = LocalPlatformConfiguration.current
    val player = remember {
        AudioPlayer(platformConfiguration)
    }
    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }
    return player
}