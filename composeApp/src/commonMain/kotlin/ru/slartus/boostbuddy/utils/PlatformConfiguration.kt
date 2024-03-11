package ru.slartus.boostbuddy.utils

expect class PlatformConfiguration {
    val platform: Platform

    fun openBrowser(url: String)
}

sealed class Platform(val name: String) {
    data object Android : Platform(name = "android")
    data object AndroidTV : Platform(name = "androidTV")
    data object iOS : Platform(name = "ios")
}