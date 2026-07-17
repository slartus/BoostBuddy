package ru.slartus.boostbuddy.utils

import kotlinx.coroutines.CoroutineScope

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
