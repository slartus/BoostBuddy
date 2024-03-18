package ru.slartus.boostbuddy.ui.widgets

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import android.webkit.CookieManager
import android.webkit.HttpAuthHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.data.ktor.USER_AGENT


@Composable
actual fun WebView(url: String, clickCoors: Offset?, onCookieChange: (String) -> Unit) {
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val webView = remember {
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
                    view?.injectCSS()
                    super.onPageFinished(view, currentUrl)
                    if (currentUrl != null)
                        onCookieChange(
                            CookieManager.getInstance().getCookie(currentUrl).orEmpty()
                        )
                }
            }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            webView
        }
    )
    LaunchedEffect(clickCoors) {
        if (clickCoors != null) {
            webView.simulateClick(scope, clickCoors.x, clickCoors.y)
        }
    }
    LaunchedEffect(url) {
        webView.loadUrl(url)
    }
}

private fun WebView.injectCSS() {
    try {
       val style = ":focus { border: 2px solid red; }"
        val script =
                "var parent = document.getElementsByTagName('head').item(0);\n" +
                "var style = document.createElement('style');\n" +
                "style.type = 'text/css';\n" +
                "style.innerHTML = \"$style\";\n" +
                "parent.appendChild(style)"
        evaluateJavascript(script, null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun WebView.simulateClick(coroutineScope: CoroutineScope, x: Float, y: Float) {
    coroutineScope.launch {
        val downTime: Long = SystemClock.uptimeMillis()
        val eventTime: Long = SystemClock.uptimeMillis()
        val properties = arrayOfNulls<PointerProperties>(1)
        val pp1 = PointerProperties()
        pp1.id = 0
        pp1.toolType = MotionEvent.TOOL_TYPE_FINGER
        properties[0] = pp1
        val pointerCoords = arrayOfNulls<PointerCoords>(1)
        val pc1 = PointerCoords()
        pc1.x = x + scrollX
        pc1.y = y + scrollY
        pc1.pressure = 1f
        pc1.size = 1f
        pointerCoords[0] = pc1
        var motionEvent = MotionEvent.obtain(
            downTime, eventTime,
            MotionEvent.ACTION_DOWN, 1, properties,
            pointerCoords, 0, 0, 1f, 1f, 0, 0, 0, 0
        )
        dispatchTouchEvent(motionEvent)
        motionEvent = MotionEvent.obtain(
            downTime, eventTime,
            MotionEvent.ACTION_UP, 1, properties,
            pointerCoords, 0, 0, 1f, 1f, 0, 0, 0, 0
        )
        dispatchTouchEvent(motionEvent)
        //flingScroll(x.toInt(), y.toInt())
        delay(300)

        clearFocus()
    }
}