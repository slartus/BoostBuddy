package ru.slartus.boostbuddy.utils

import android.webkit.CookieManager
import android.webkit.ValueCallback
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual object WebManager {
    actual suspend fun clearWebViewCookies() = suspendCoroutine { continuation ->
        val callback = ValueCallback<Boolean> { _ -> continuation.resume(Unit) }

        CookieManager.getInstance().removeAllCookies(callback)
    }
}