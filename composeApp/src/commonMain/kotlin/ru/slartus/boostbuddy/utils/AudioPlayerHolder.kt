package ru.slartus.boostbuddy.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AudioPlayerHolder(
    private val platformConfiguration: PlatformConfiguration,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    private val listener = object : AudioPlayerStateListener {
        override fun onStateChanged(state: AudioPlayerState) {
            _state.update { it.copy(state = state) }
        }
    }

    private val player: AudioPlayer by lazy {
        AudioPlayer(platformConfiguration, scope, listener)
    }

    fun play(trackKey: String, url: String) {
        val current = _state.value
        if (current.trackKey == trackKey) {
            when (current.state) {
                is AudioPlayerState.Playing,
                AudioPlayerState.Preparing,
                AudioPlayerState.Prepared,
                AudioPlayerState.Starting -> return

                is AudioPlayerState.Paused -> {
                    player.resume()
                    return
                }

                AudioPlayerState.Completed,
                AudioPlayerState.Error,
                AudioPlayerState.Idle,
                AudioPlayerState.Stopped -> Unit
            }
        } else if (current.state.isActive) {
            player.pause()
        }
        _state.value = AudioPlaybackState(trackKey = trackKey, state = AudioPlayerState.Idle)
        player.play(url)
    }

    fun pause(trackKey: String) {
        if (_state.value.trackKey != trackKey) return
        player.pause()
    }

    fun resume(trackKey: String) {
        if (_state.value.trackKey != trackKey) return
        player.resume()
    }

    fun seekTo(trackKey: String, positionSeconds: Int) {
        if (_state.value.trackKey != trackKey) return
        player.seekTo(positionSeconds)
    }
}

data class AudioPlaybackState(
    val trackKey: String? = null,
    val state: AudioPlayerState = AudioPlayerState.Idle,
)

private val AudioPlayerState.isActive: Boolean
    get() = when (this) {
        is AudioPlayerState.Playing,
        is AudioPlayerState.Paused,
        AudioPlayerState.Prepared,
        AudioPlayerState.Preparing,
        AudioPlayerState.Starting -> true

        AudioPlayerState.Completed,
        AudioPlayerState.Error,
        AudioPlayerState.Idle,
        AudioPlayerState.Stopped -> false
    }
