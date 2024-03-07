package ru.slartus.boostbuddy.utils

import platform.Foundation.NSDate
import platform.Foundation.NSHTTPCookie
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.WebKit.WKHTTPCookieStore
import platform.WebKit.WKWebsiteDataStore
import platform.WebKit.WKWebsiteDataTypeCookies

actual object WebManager {
    actual suspend fun clearWebViewCookies() {
        val cookieStore: WKHTTPCookieStore =
            WKWebsiteDataStore.defaultDataStore().httpCookieStore
        cookieStore.getAllCookies { cookies ->
            cookies?.filterIsInstance<NSHTTPCookie>()?.forEach { cookie ->
                cookieStore.deleteCookie(cookie, null)
            }
        }
        WKWebsiteDataStore.defaultDataStore().removeDataOfTypes(
            setOf(
                WKWebsiteDataTypeCookies
            ),
            NSDate.dateWithTimeIntervalSince1970(0.0)
        ) { }
    }
}