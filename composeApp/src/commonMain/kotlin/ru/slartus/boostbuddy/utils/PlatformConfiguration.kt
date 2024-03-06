package ru.slartus.boostbuddy.utils

expect class PlatformConfiguration {
    val platform: Platform
}

sealed class Platform(val name: String) {
    data object Android : Platform(name = "android")
    data object iOS : Platform(name = "ios")
}