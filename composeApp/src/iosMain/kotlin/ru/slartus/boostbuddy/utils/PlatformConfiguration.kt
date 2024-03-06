package ru.slartus.boostbuddy.utils

actual open class PlatformConfiguration private constructor() {
    actual val platform: Platform = Platform.iOS
}