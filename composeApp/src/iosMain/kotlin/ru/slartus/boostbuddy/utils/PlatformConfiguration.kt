package ru.slartus.boostbuddy.utils

import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual open class PlatformConfiguration private constructor() {
    actual val platform: Platform = Platform.iOS

    actual val appVersion: String by lazy(LazyThreadSafetyMode.NONE) {
        infoValue(VERSION_KEY) ?: "1.0"
    }

    actual fun openBrowser(url: String) {
        NSURL.URLWithString(url)?.let {
            UIApplication.sharedApplication().openURL(it)
        }
    }

    companion object : PlatformConfiguration() {
        private const val VERSION_KEY = "CFBundleShortVersionString"
        @Suppress("unused")
        val shared = PlatformConfiguration()

        private fun infoValue(key: String): String? {
            return NSBundle.mainBundle.infoDictionary?.get(key)?.toString()
        }
    }
}