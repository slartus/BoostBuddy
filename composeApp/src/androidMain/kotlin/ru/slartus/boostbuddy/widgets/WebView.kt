package ru.slartus.boostbuddy.widgets

import android.graphics.Bitmap
import android.util.Log
import android.webkit.CookieManager
import android.webkit.HttpAuthHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun WebView(url: String, onCookieChange: (String) -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    userAgentString =
                        "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
                }
                webViewClient = object : WebViewClient() {
                    override fun onReceivedHttpAuthRequest(
                        view: WebView?,
                        handler: HttpAuthHandler?,
                        host: String?,
                        realm: String?,
                    ) {

                    }

                    override fun onReceivedLoginRequest(
                        view: WebView?,
                        realm: String?,
                        account: String?,
                        args: String?
                    ) {

                    }

                    override fun onPageStarted(webView: WebView, url: String?, favicon: Bitmap?) {
                        if (url != null)
                            onCookieChange(CookieManager.getInstance().getCookie(url))
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        val cookie = CookieManager.getInstance().getCookie(request.url.toString())
                        Log.e("cookie", cookie)
                        return false
                    }

                    override fun onPageFinished(view: WebView?, currentUrl: String?) {
                        super.onPageFinished(view, currentUrl)
                        if (currentUrl != null)
                            onCookieChange(CookieManager.getInstance().getCookie(currentUrl))
                    }

                }
            }
        }
    ) { view ->
        view.loadUrl(url)
    }
}