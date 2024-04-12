package ru.slartus.boostbuddy.utils

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

// https://developer.android.com/reference/android/media/MediaPlayer
actual class AudioPlayer actual constructor(
    private val platformConfiguration: PlatformConfiguration,
    private val coroutineScope: CoroutineScope,
    private val listener: AudioPlayerStateListener
) {
    private var timerJob: Job = Job(null)
    private var mediaPlayer: MediaPlayer? = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build()
        )
        setOnCompletionListener {
            listener.onStateChanged(AudioPlayerState.Completed)
        }
        setOnErrorListener { player, what, extra ->
            listener.onStateChanged(AudioPlayerState.Error)
            false // OnCompletionListener to be called
        }
        setOnPreparedListener { player ->
            listener.onStateChanged(AudioPlayerState.Prepared)
            player.start()
            startTimer()
        }
    }

    actual fun play(url: String) {
        mediaPlayer?.apply {
            setDataSource(platformConfiguration.androidContext, Uri.parse(url))
            listener.onStateChanged(AudioPlayerState.Preparing)
            prepareAsync()
        }
    }

    actual fun pause() {
        stopTimer()
        mediaPlayer?.apply {
            if (isPlaying)
                pause()
            listener.onStateChanged(AudioPlayerState.Paused(getCurrentPositionSeconds()))
        }
    }

    actual fun resume() {
        mediaPlayer?.apply {
            this.start()
            startTimer()
        }
    }

    actual fun release() {
        stopTimer()
        mediaPlayer?.release()
        mediaPlayer = null
        listener.onStateChanged(AudioPlayerState.Stopped)
    }

    private fun startTimer() {
        timerJob = coroutineScope.launch(SupervisorJob()) {
            while (true) {
                mediaPlayer?.apply {
                    if (isPlaying)
                        listener.onStateChanged(AudioPlayerState.Playing(getCurrentPositionSeconds()))
                }
                delay(1.seconds)
            }
        }
    }

    private fun getCurrentPositionSeconds(): Int =
        mediaPlayer?.let { it.currentPosition / 1000 } ?: 0

    private fun stopTimer() {
        timerJob.cancel()
    }

    actual fun seekTo(position: Int) {
        mediaPlayer?.apply {
            seekTo(position * 1000)
            if (isPlaying)
                listener.onStateChanged(AudioPlayerState.Playing(getCurrentPositionSeconds()))
            else
                listener.onStateChanged(AudioPlayerState.Paused(getCurrentPositionSeconds()))
        }
    }
}