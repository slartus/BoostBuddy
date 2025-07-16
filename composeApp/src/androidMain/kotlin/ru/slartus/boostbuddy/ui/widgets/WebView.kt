package ru.slartus.boostbuddy.ui.widgets

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import android.view.View
import android.webkit.CookieManager
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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


@Composable
actual fun WebView(url: String, clickCoors: Offset?, onCookieChange: (String) -> Unit) {
    val isAtv = LocalPlatformConfiguration.current.platform == Platform.AndroidTV
    val scope = rememberCoroutineScope()
    var buttons by remember { mutableStateOf<ImmutableList<HtmlButton>>(persistentListOf()) }
    val context = LocalContext.current
    var buttonsJob: Job by remember { mutableStateOf(Job(null)) }
    var webViewFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val scanButtons: (view: WebView?) -> Unit = { view ->
        buttonsJob.cancel()
        buttonsJob = scope.launch(SupervisorJob()) {
            buttons =
                view?.getAllButtons().orEmpty()
                    .distinctBy { it.id }
                    .sortedBy { it.text ?: "яяя" }
                    .toImmutableList()
        }
    }
    val webView = remember {
        WebView(context).apply {
            this.setOnFocusChangeListener { _, b ->
                webViewFocused = b
            }
            settings.apply {
                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                domStorageEnabled = true
                setSupportMultipleWindows(true)
                userAgentString = USER_AGENT
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(webView: WebView, url: String?, favicon: Bitmap?) {
                    if (url != null)
                        onCookieChange(CookieManager.getInstance().getCookie(url).orEmpty())
                }

                override fun onLoadResource(view: WebView?, url: String?) {
                    if (url != null)
                        onCookieChange(CookieManager.getInstance().getCookie(url).orEmpty())
                    if (isAtv) {
                        scanButtons(this@apply)
                    }
                }

                override fun onPageFinished(view: WebView?, currentUrl: String?) {
                    if (isAtv)
                        view?.injectCSS()
                    super.onPageFinished(view, currentUrl)
                    if (currentUrl != null)
                        onCookieChange(CookieManager.getInstance().getCookie(currentUrl).orEmpty())
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
                item {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { scanButtons(webView) }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "rescan",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                items(buttons, key = { it.id }) { button ->
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                if (button.isInput) {
                                    webView.click( 0f, 0f)
                                    webView.evaluateJavascript(
                                        "setTimeout(function() {" +
                                                "document.getElementById(\"${button.id}\").focus();" +
                                                "document.getElementById(\"${button.id}\").click();" +
                                                "}, 1000);",

                                        null
                                    )
                                } else {
                                    webView.evaluateJavascript(
                                        "document.getElementById(\"${button.id}\").click();",
                                        null
                                    )
                                }
                            }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = button.nodeText,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
        AndroidView(
            modifier = Modifier
                .focusRequester(focusRequester)
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
//        val style = ":focus { border: 2px solid red; }"
//        val script =
//            "var parent = document.getElementsByTagName('head').item(0);\n" +
//                    "var style = document.createElement('style');\n" +
//                    "style.type = 'text/css';\n" +
//                    "style.innerHTML = \"$style\";\n" +
//                    "parent.appendChild(style)"
//        evaluateJavascript(script, null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private suspend fun WebView.getAllButtons(): List<HtmlButton> = suspendCoroutine { continuation ->
    val script =
        "Array.from(document.querySelectorAll(\"button, input, [role='button']\")).forEach(b => b.id = b.id||(Date.now().toString(36) + Math.random().toString(36).substr(2)));" +
                "return Array.from(document.querySelectorAll(\"button, input, [role='button']\")).map(function(b){return {id: b.id, node: b.nodeName, text: b.innerText||b.placeholder||b.getAttribute('data-provider')}});"

    val functionScript = "(function() { $script; })();"
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    evaluateJavascript(functionScript) { result ->
        runCatching {
            val buttons = json.decodeFromString(
                ListSerializer(HtmlButton.serializer()),
                result
            )
            continuation.resume(buttons)
        }.onFailure { continuation.resumeWithException(it) }
    }
}

@Serializable
private data class HtmlButton(val id: String, val text: String?, val node: String) {
    val isInput: Boolean = node.lowercase() == "input"
    val nodeText: String = if (isInput) "[${text ?: id}]" else text ?: id
}

private fun View.click(x: Float, y: Float) {
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
}

private fun View.simulateClick(coroutineScope: CoroutineScope, x: Float, y: Float) {
    coroutineScope.launch {
        click(x, y)
        //flingScroll(x.toInt(), y.toInt())
        delay(300)

        clearFocus()
    }
}