package ru.slartus.boostbuddy.utils

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual open class PlatformConfiguration private constructor() {
    actual val platform: Platform = Platform.iOS

    actual fun openBrowser(url: String) {
        NSURL.URLWithString(url)?.let {
            UIApplication.sharedApplication().openURL(it)
        }
    }

    companion object : PlatformConfiguration() {
        @Suppress("unused")
        val shared = PlatformConfiguration()
    }
}