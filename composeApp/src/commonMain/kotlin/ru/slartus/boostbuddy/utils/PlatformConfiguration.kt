package ru.slartus.boostbuddy.utils

import kotlinx.io.files.Path

expect class PlatformConfiguration {
    val platform: Platform
    val appVersion: String
    val isDebug: Boolean
    fun openBrowser(url: String)
    fun installApp(path: Path)
}

sealed class Platform(val name: String) {
    data object Android : Platform(name = "android")
    data object AndroidTV : Platform(name = "androidTV")
    data object iOS : Platform(name = "ios")
}