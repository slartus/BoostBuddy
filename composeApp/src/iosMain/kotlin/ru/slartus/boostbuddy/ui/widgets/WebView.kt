package ru.slartus.boostbuddy.ui.widgets

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSHTTPCookie
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLAuthenticationMethodNTLM
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.create
import platform.Foundation.serverTrust
import platform.WebKit.WKHTTPCookieStore
import platform.WebKit.WKHTTPCookieStoreObserverProtocol
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject
import ru.slartus.boostbuddy.data.ktor.USER_AGENT

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun WebView(url: String, clickCoors: Offset?, onCookieChange: (String) -> Unit) {

    val cookiesObserver = remember {
        CookieStoreObserver {
            onCookieChange(it)
        }
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
                customUserAgent = USER_AGENT
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
    override fun webView(
        webView: WKWebView,
        didReceiveAuthenticationChallenge: NSURLAuthenticationChallenge,
        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit,
    ) {
        if (didReceiveAuthenticationChallenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodNTLM) {
            completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
        } else {
            didReceiveAuthenticationChallenge.decideSslChallenge(completionHandler)
        }
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun webView(webView: WKWebView, didCommitNavigation: WKNavigation?) {
        webView.URL?.absoluteString?.let { onPageStarted(it, webView.title) }
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        webView.URL?.absoluteString?.let { onPageLoaded(it, webView.title) }
    }

    companion object {
        @OptIn(ExperimentalForeignApi::class)
        private fun NSURLAuthenticationChallenge.decideSslChallenge(completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit) {
            if (protectionSpace.authenticationMethod != NSURLAuthenticationMethodServerTrust) {
                completionHandler(1, null)
                return
            }
            val hosts = listOf("boosty.to", "google.com")
            if (!hosts.any { protectionSpace.host.contains(it) }) {
                completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
                return
            }
            val serverTrust = protectionSpace.serverTrust
            if (serverTrust != null) {
                val credential = NSURLCredential.create(trust = serverTrust)
                completionHandler(NSURLSessionAuthChallengeUseCredential, credential)
            } else {
                completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
            }
        }
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

private fun WKWebView.loadRequest(url: String) {
    val request = NSMutableURLRequest.requestWithURL(URL = NSURL(string = url))
    loadRequest(request = request)
}