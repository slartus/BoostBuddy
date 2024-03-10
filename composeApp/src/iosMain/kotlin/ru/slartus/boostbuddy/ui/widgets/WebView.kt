package ru.slartus.boostbuddy.ui.widgets

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSHTTPCookie
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.WebKit.WKHTTPCookieStore
import platform.WebKit.WKHTTPCookieStoreObserverProtocol
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun WebView(url: String, onCookieChange: (String) -> Unit) {

    val cookiesObserver = remember {
        CookieStoreObserver({
            println(">>>>>>>>>>>>>>>>CookieStoreObserver $it")
            onCookieChange(it)
        })
    }
    DisposableEffect(Unit) {
        WKWebsiteDataStore.defaultDataStore().httpCookieStore.addObserver(cookiesObserver)
        onDispose {
            WKWebsiteDataStore.defaultDataStore().httpCookieStore.removeObserver(cookiesObserver)
        }
    }
    val rememberedNavigationDelegate = remember {
        WKNavigationDelegate(
            onPageLoaded = { _, _ ->

            },
            onPageStarted = { _, _ ->

            }
        )
    }
    UIKitView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            val config = WKWebViewConfiguration().apply {
                limitsNavigationsToAppBoundDomains = true
            }
            WKWebView(frame = CGRectZero.readValue(), configuration = config).apply {
                navigationDelegate = rememberedNavigationDelegate
                loadRequest(url)
            }
        },
        onRelease = { webView ->
            webView.navigationDelegate = null
            // webView.removeObservers(observer, setOf(WKWebViewKeyPath.ESTIMATED_PROGRESS, WKWebViewKeyPath.URL))
        }
    )
}

@Suppress("CONFLICTING_OVERLOADS")
class WKNavigationDelegate(
    private val onPageStarted: (url: String?, title: String?) -> Unit = { _, _ -> },
    private val onPageLoaded: (url: String?, title: String?) -> Unit = { _, _ -> },
) : NSObject(), WKNavigationDelegateProtocol {

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun webView(webView: WKWebView, didCommitNavigation: WKNavigation?) {
        webView.URL?.absoluteString?.let { onPageStarted(it, webView.title) }
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        webView.URL?.absoluteString?.let { onPageLoaded(it, webView.title) }
    }
}

class CookieStoreObserver(
    private val onCookiesChange: (cookies: String) -> Unit = { }
) : NSObject(), WKHTTPCookieStoreObserverProtocol {
    override fun cookiesDidChangeInCookieStore(cookieStore: WKHTTPCookieStore) {
        cookieStore.getAllCookies { cookies ->
            cookies?.filterIsInstance<NSHTTPCookie>()
                ?.joinToString(separator = "; ") { cookie ->
                    "${cookie.name}=${cookie.value}"
                }?.let {
                    onCookiesChange(it)
                }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun getCookie(url: String): String? = suspendCancellableCoroutine { continuation ->
    val cookieStore: WKHTTPCookieStore = WKWebsiteDataStore.defaultDataStore().httpCookieStore

    var result: String? = null
    cookieStore.getAllCookies { cookies ->
        continuation.resume(
            cookies?.filterIsInstance<NSHTTPCookie>()
                ?.joinToString(separator = "; ") { cookie ->
                    "${cookie.name}=${cookie.value}"
                }) {}
    }
}

private fun WKWebView.loadRequest(url: String) {
    val request = NSMutableURLRequest.requestWithURL(URL = NSURL(string = url))
    loadRequest(request = request)
}