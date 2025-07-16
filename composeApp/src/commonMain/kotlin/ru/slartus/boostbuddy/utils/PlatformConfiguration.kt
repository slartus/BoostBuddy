package ru.slartus.boostbuddy.utils

import kotlinx.io.files.Path

expect class PlatformConfiguration {
    val platform: Platform
    val appVersion: String
    val isDebug: Boolean
    fun openBrowser(url: String, onError: (() -> Unit)? = null)
    fun installApp(path: Path)
    fun shareText(text: String, onError: (() -> Unit)?)
    fun shareFile(path: Path)
}

sealed class Platform(val name: String) {
    data object Android : Platform(name = "android")
    data object AndroidTV : Platform(name = "androidTV")
    data object iOS : Platform(name = "ios")

    val isPhone: Boolean
        get() = when (this) {
            Android -> true
            AndroidTV -> false
            iOS -> true
        }

    val isTV: Boolean
        get() = when (this) {
            Android -> false
            AndroidTV -> true
            iOS -> false
        }
}