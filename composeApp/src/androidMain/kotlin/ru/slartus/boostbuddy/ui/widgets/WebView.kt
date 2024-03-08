package ru.slartus.boostbuddy.ui.widgets

import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.HttpAuthHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import ru.slartus.boostbuddy.data.ktor.USER_AGENT

@Composable
actual fun WebView(url: String, onCookieChange: (String) -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    userAgentString = USER_AGENT
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
                            onCookieChange(CookieManager.getInstance().getCookie(url).orEmpty())
                    }

                    override fun onLoadResource(view: WebView?, url: String?) {
                        if (url != null)
                            onCookieChange(CookieManager.getInstance().getCookie(url).orEmpty())
                    }

                    override fun onPageFinished(view: WebView?, currentUrl: String?) {
                        super.onPageFinished(view, currentUrl)
                        if (currentUrl != null)
                            onCookieChange(CookieManager.getInstance().getCookie(currentUrl).orEmpty())
                    }

                }
            }
        }
    ) { view ->
        view.loadUrl(url)
    }
}