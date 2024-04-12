package ru.slartus.boostbuddy.utils

import kotlinx.coroutines.CoroutineScope

actual class AudioPlayer actual constructor(
    platformConfiguration: PlatformConfiguration,
    coroutineScope: CoroutineScope,
    listener: AudioPlayerStateListener
){
    actual fun play(url: String) {

    }

    actual fun pause() {

    }

    actual fun resume() {

    }

    actual fun release() {

    }

    actual fun seekTo(position: Int) {
    }
}