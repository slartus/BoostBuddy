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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import ru.slartus.boostbuddy.data.ktor.USER_AGENT
import ru.slartus.boostbuddy.ui.common.BackHandlerEffect
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration
import ru.slartus.boostbuddy.utils.Platform
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.seconds


@Composable
actual fun WebView(url: String, clickCoors: Offset?, onCookieChange: (String) -> Unit) {
    val isAtv = LocalPlatformConfiguration.current.platform == Platform.AndroidTV
    val scope = rememberCoroutineScope()
    var buttons by remember { mutableStateOf<ImmutableList<HtmlButton>>(persistentListOf()) }
    val context = LocalContext.current
    var buttonsJob: Job by remember { mutableStateOf(Job(null)) }
    var webViewFocused by remember { mutableStateOf(false) }
    val webView = remember {
        WebView(context).apply {
            this.setOnFocusChangeListener { view, b ->
                webViewFocused = b
            }
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
                    if (isAtv) {
                        buttonsJob.cancel()
                        buttonsJob = scope.launch(SupervisorJob()) {
                            delay(1.seconds)
                            buttons =
                                view?.getAllButtons().orEmpty()
                                    .filter { !it.text.isNullOrEmpty() }
                                    .distinctBy { it.id }
                                    .toImmutableList()
                        }
                    }
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

    BackHandlerEffect(enabled = webViewFocused) {
        if (webViewFocused)
            webView.clearFocus()
    }

    Column {
        if (isAtv) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(buttons, key = { it.id }) { button ->
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                webView.evaluateJavascript(
                                    "document.getElementById(\"${button.id}\").click();",
                                    null
                                )
                            }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = button.text.orEmpty(),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
        AndroidView(
            modifier = Modifier
                .weight(1f),
            factory = {
                webView
            }
        )
    }
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

private suspend fun WebView.getAllButtons(): List<HtmlButton> = suspendCoroutine { continuation ->
    val script =
        "Array.from(document.querySelectorAll(\"button, [role='button']\")).forEach(b=>b.id = b.id||(Date.now().toString(36) + Math.random().toString(36).substr(2)));" +
                "return Array.from(document.querySelectorAll(\"button, [role='button']\")).map(function(b){return {id: b.id, text: b.innerText||b.getAttribute('data-provider')}});"

    val functionScript = "(function() { $script; })();"
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    evaluateJavascript(functionScript) { result ->
        runCatching {
            continuation.resume(
                json.decodeFromString(
                    ListSerializer(HtmlButton.serializer()),
                    result
                )
            )
        }.onFailure { continuation.resumeWithException(it) }
    }
}

@Serializable
private data class HtmlButton(val id: String, val text: String?)

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