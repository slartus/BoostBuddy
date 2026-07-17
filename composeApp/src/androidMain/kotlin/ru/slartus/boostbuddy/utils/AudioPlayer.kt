package ru.slartus.boostbuddy.utils

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

actual class AudioPlayer actual constructor(
    private val platformConfiguration: PlatformConfiguration,
    private val coroutineScope: CoroutineScope,
    private val listener: AudioPlayerStateListener
) {
    private var timerJob: Job? = null
    private var currentUrl: String? = null
    private var player: ExoPlayer? = null

    actual fun play(url: String) {
        stopTimer()
        val player = obtainPlayer()
        listener.onStateChanged(AudioPlayerState.Preparing)
        if (url == currentUrl && player.playbackState == Player.STATE_IDLE && player.mediaItemCount > 0) {
            player.prepare()
            player.playWhenReady = true
            return
        }
        currentUrl = url
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    actual fun pause() {
        stopTimer()
        val player = player ?: return
        if (!player.playWhenReady) return
        player.pause()
        listener.onStateChanged(AudioPlayerState.Paused(getCurrentPositionSeconds()))
    }

    actual fun resume() {
        val player = player ?: return
        player.playWhenReady = true
        if (player.playbackState == Player.STATE_IDLE) {
            player.prepare()
            listener.onStateChanged(AudioPlayerState.Preparing)
        } else {
            player.play()
        }
    }

    actual fun release() {
        stopTimer()
        player?.release()
        player = null
        currentUrl = null
        listener.onStateChanged(AudioPlayerState.Stopped)
    }

    actual fun seekTo(position: Int) {
        val player = player ?: return
        player.seekTo(position * 1000L)
        if (player.isPlaying)
            listener.onStateChanged(AudioPlayerState.Playing(getCurrentPositionSeconds()))
        else
            listener.onStateChanged(AudioPlayerState.Paused(getCurrentPositionSeconds()))
    }

    private fun obtainPlayer(): ExoPlayer = player ?: createPlayer().also { player = it }

    private fun createPlayer(): ExoPlayer =
        ExoPlayer.Builder(platformConfiguration.androidContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply { addListener(PlayerListener()) }

    private inner class PlayerListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> listener.onStateChanged(AudioPlayerState.Preparing)

                Player.STATE_READY -> {
                    val player = player ?: return
                    if (!player.playWhenReady) {
                        listener.onStateChanged(AudioPlayerState.Paused(getCurrentPositionSeconds()))
                    }
                }

                Player.STATE_ENDED -> {
                    stopTimer()
                    listener.onStateChanged(AudioPlayerState.Completed)
                }

                Player.STATE_IDLE -> Unit
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                listener.onStateChanged(AudioPlayerState.Playing(getCurrentPositionSeconds()))
                startTimer()
            } else {
                stopTimer()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Napier.e(tag = LOG_TAG, throwable = error) {
                "playback error code=${error.errorCodeName} url=$currentUrl"
            }
            stopTimer()
            listener.onStateChanged(AudioPlayerState.Error)
        }
    }

    private fun startTimer() {
        stopTimer()
        timerJob = coroutineScope.launch {
            while (true) {
                player?.let {
                    if (it.isPlaying)
                        listener.onStateChanged(AudioPlayerState.Playing(getCurrentPositionSeconds()))
                }
                delay(1.seconds)
            }
        }
    }

    private fun getCurrentPositionSeconds(): Int =
        player?.let { (it.currentPosition / 1000).toInt() } ?: 0

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private companion object {
        const val LOG_TAG = "AudioPlayer"
    }
}
