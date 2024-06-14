package ru.slartus.boostbuddy.utils

expect class VideoPlayer() {
    fun playUrl(platformConfiguration: PlatformConfiguration, title: String, url: String, mimeType: String?, posterUrl: String?)
}