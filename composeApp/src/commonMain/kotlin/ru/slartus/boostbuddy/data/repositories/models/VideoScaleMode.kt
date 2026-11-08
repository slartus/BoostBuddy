package ru.slartus.boostbuddy.data.repositories.models

enum class VideoScaleMode {
    Fit,
    Fill,
    Stretch;

    companion object {
        val Default: VideoScaleMode = Fit
    }
}
