package ru.slartus.boostbuddy.utils

import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual object WebManager {
    actual suspend fun clearWebViewCookies() = suspendCoroutine { continuation ->
        val callback = ValueCallback<Boolean> { _ -> continuation.resume(Unit) }

        CookieManager.getInstance().removeAllCookies(callback)
    }

    actual suspend fun clearWebViewStorage() {
        WebStorage.getInstance().deleteAllData()
    }

    actual suspend fun getCookies(url: String): String? = withContext(Dispatchers.IO) {
        CookieManager.getInstance().getCookie(url)
    }
}