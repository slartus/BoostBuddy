package ru.slartus.boostbuddy.utils

import android.util.AndroidRuntimeException
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual object WebManager {
    private const val MISSING_WEBVIEW_PACKAGE_EXCEPTION =
        "android.webkit.WebViewFactory\$MissingWebViewPackageException"
    private const val MAX_CAUSE_DEPTH = 10

    actual suspend fun clearWebViewCookies() = suspendCoroutine { continuation ->
        val callback = ValueCallback<Boolean> { _ -> continuation.resume(Unit) }

        CookieManager.getInstance().removeAllCookies(callback)
    }

    actual suspend fun clearWebViewStorage() {
        WebStorage.getInstance().deleteAllData()
    }

    actual suspend fun getCookies(url: String): String? = withContext(Dispatchers.Main) {
        try {
            CookieManager.getInstance().getCookie(url)
        } catch (e: AndroidRuntimeException) {
            if (e.isMissingWebViewPackage()) throw WebViewUnavailableException(e) else throw e
        }
    }

    private fun Throwable.isMissingWebViewPackage(): Boolean =
        generateSequence(this) { it.cause }
            .take(MAX_CAUSE_DEPTH)
            .any { it.javaClass.name == MISSING_WEBVIEW_PACKAGE_EXCEPTION }
}
