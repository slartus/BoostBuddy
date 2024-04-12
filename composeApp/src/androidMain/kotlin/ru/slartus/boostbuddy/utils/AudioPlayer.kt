package ru.slartus.boostbuddy.utils

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri

actual class AudioPlayer actual constructor(private val platformConfiguration: PlatformConfiguration) {
    private var mediaPlayer: MediaPlayer? = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                .build()
        )
        setOnPreparedListener { player -> player.start() }
    }

    actual fun play(url: String) {
        mediaPlayer?.apply {
            setDataSource(platformConfiguration.androidContext, Uri.parse(url))
            prepareAsync()
        }
    }

    actual fun pause() {
        mediaPlayer?.apply {
            if(isPlaying)
                pause()
        }
    }

    actual fun resume() {
        mediaPlayer?.apply {
            this.start()
        }
    }

    actual fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
