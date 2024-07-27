package ru.slartus.boostbuddy.utils

import kotlinx.io.files.Path
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual open class PlatformConfiguration private constructor() {
    actual val platform: Platform = Platform.iOS

    actual val appVersion: String by lazy(LazyThreadSafetyMode.NONE) {
        infoValue(VERSION_KEY) ?: "1.0"
    }
    actual val isDebug: Boolean by lazy(LazyThreadSafetyMode.NONE) {
        infoValue(CONFIGURATION_KEY)?.startsWith("debug", ignoreCase = true) ?: false
    }

    actual fun openBrowser(url: String, onError: (() -> Unit)?) {
        NSURL.URLWithString(url)?.let {
            UIApplication.sharedApplication().openURL(it)
        }
    }

    actual fun installApp(path: Path) {
        TODO()
    }

    companion object : PlatformConfiguration() {
        private const val VERSION_KEY = "CFBundleShortVersionString"
        private const val CONFIGURATION_KEY = "Configuration"

        @Suppress("unused")
        val shared = PlatformConfiguration()

        private fun infoValue(key: String): String? {
            return NSBundle.mainBundle.infoDictionary?.get(key)?.toString()
        }
    }

    actual fun shareText(text: String, onError: (() -> Unit)?) {
        TODO()
    }

    actual fun shareFile(path: Path) {
        TODO()
    }
}