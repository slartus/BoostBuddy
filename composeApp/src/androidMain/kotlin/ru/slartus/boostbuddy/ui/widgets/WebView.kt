package ru.slartus.boostbuddy.ui.widgets

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.slartus.boostbuddy.data.ktor.USER_AGENT
import ru.slartus.boostbuddy.ui.common.BackHandlerEffect
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration
import ru.slartus.boostbuddy.utils.Platform
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


@Composable
actual fun WebView(url: String, clickCoors: Offset?, onCookieChange: (String) -> Unit) {
    val isAtv = LocalPlatformConfiguration.current.platform == Platform.AndroidTV
    val scope = rememberCoroutineScope()
    var buttons by remember { mutableStateOf<ImmutableList<HtmlButton>>(persistentListOf()) }
    val context = LocalContext.current
    val webViewMajorVersion = remember(context) {
        val ua = WebSettings.getDefaultUserAgent(context)
        Regex("""Chrome/(\d+)""").find(ua)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }
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
                override fun onLoadResource(view: WebView?, url: String?) {
                    if (url != null)
                        onCookieChange(CookieManager.getInstance().getCookie(url).orEmpty())
                    if (isAtv) {
                        scanButtons(this@apply)
                    }
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler?,
                    error: SslError?
                ) {
                    Log.w(
                        WV_TAG,
                        "onReceivedSslError: primary=${error?.primaryError} url=${error?.url} cert=${error?.certificate?.issuedTo?.cName}"
                    )
                    handler?.proceed()
                }

                override fun onPageStarted(webView: WebView, url: String?, favicon: Bitmap?) {
                    if (url != null)
                        onCookieChange(CookieManager.getInstance().getCookie(url).orEmpty())
                }

                override fun onPageFinished(view: WebView?, currentUrl: String?) {
                    super.onPageFinished(view, currentUrl)
                    if (currentUrl != null)
                        onCookieChange(CookieManager.getInstance().getCookie(currentUrl).orEmpty())
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    Log.e(
                        WV_TAG,
                        "onReceivedError: code=$errorCode desc=$description url=$failingUrl"
                    )
                }

                // Перехватываем HTML-документы и инжектируем полифиллы в <head>
                // ПЕРЕД любыми скриптами страницы. Работает только на Android 7 (API ≤ 25),
                // где Chromium не поддерживает globalThis, queueMicrotask и др.
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? =
                    injectPolyfillsIfNeeded(request) ?: super.shouldInterceptRequest(view, request)
            }
            webChromeClient = object : WebChromeClient() {
                // JS console → logcat
                override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                    val level = when (msg.messageLevel()) {
                        ConsoleMessage.MessageLevel.ERROR -> Log.ERROR
                        ConsoleMessage.MessageLevel.WARNING -> Log.WARN
                        else -> Log.DEBUG
                    }
                    Log.println(
                        level,
                        WV_JS_TAG,
                        "${msg.message()} [${msg.sourceId()}:${msg.lineNumber()}]"
                    )
                    return true
                }
            }
        }
    }

    BackHandlerEffect(enabled = webViewFocused) {
        if (webViewFocused)
            webView.clearFocus()
    }

    Column {
        // boosty.to использует type="module" (ES modules) — требует Chrome 63+.
        // На старых WebView страница загружается, но JS не работает.
        if (webViewMajorVersion in 1..62) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = "Ваш Android System WebView устарел (версия $webViewMajorVersion, требуется 63+). " +
                            "Обновите приложение «Android System WebView» в Google Play — после этого авторизация заработает.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
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
                                    webView.click(0f, 0f)
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

// Перехватываем HTML-документы и инжектируем полифиллы в <head>
// ПЕРЕД любыми скриптами страницы. Работает только на Android 7 (API ≤ 25),
// где Chromium не поддерживает globalThis, queueMicrotask и др.
// Возвращает null, если перехват не нужен — вызывающий должен делегировать super.
private fun injectPolyfillsIfNeeded(request: WebResourceRequest?): WebResourceResponse? {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) return null
    val accept = request?.requestHeaders?.get("Accept") ?: ""
    if (!accept.contains("text/html")) return null
    val urlString = request?.url?.toString() ?: return null
    return try {
        val okRequest = Request.Builder()
            .url(urlString)
            .also { builder ->
                request.requestHeaders.forEach { (k, v) -> builder.header(k, v) }
                val cookies = CookieManager.getInstance().getCookie(urlString)
                if (!cookies.isNullOrEmpty()) builder.header("Cookie", cookies)
            }
            .build()
        val response = polyfillOkHttpClient.newCall(okRequest).execute()
        if (!response.isSuccessful) {
            Log.w(
                WV_TAG,
                "injectPolyfillsIfNeeded non-2xx (${response.code}), falling back to WebView"
            )
            return null
        }
        val body = response.body ?: return null
        val rawContentType = response.header("Content-Type") ?: "text/html"
        val mimeType = rawContentType.split(";").first().trim()
        val charset = rawContentType.split("charset=").getOrNull(1)?.trim() ?: "utf-8"
        // Chrome 53 не поддерживает type="module" → скрипт игнорируется.
        // Убираем атрибут чтобы скрипт загрузился как обычный defer-скрипт.
        // Помогает Chrome 63+ (dynamic import поддерживается).
        var html = body.string()
        html = html.replace(Regex("""\s*type="module""""), "")
        val headMatch = Regex("<head[^>]*>").find(html)
        val modified = if (headMatch != null) {
            html.substring(0, headMatch.range.last + 1) +
                    android7Polyfill +
                    html.substring(headMatch.range.last + 1)
        } else html
        WebResourceResponse(mimeType, charset, modified.byteInputStream(Charsets.UTF_8))
    } catch (e: Exception) {
        Log.e(WV_TAG, "injectPolyfillsIfNeeded exception: ${e.message}", e)
        null
    }
}

private const val WV_TAG = "BoostWebView"
private const val WV_JS_TAG = "BoostWebView.JS"

// OkHttpClient для скачивания HTML в shouldInterceptRequest на Android 7.
// Использует okhttp3 (не com.android.okhttp) — он поддерживает современные
// cipher suites и TLS 1.2 там, где системный HttpURLConnection ломается.
private val polyfillOkHttpClient: OkHttpClient by lazy {
    @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
    val trustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
    val sslContext = SSLContext.getInstance("TLS").also {
        it.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
    }
    OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustManager)
        .hostnameVerifier { _, _ -> true }
        .followRedirects(false)
        .build()
}

// Полифилл для Android 7 (Chromium < 71): globalThis, queueMicrotask, Promise.allSettled
private val android7Polyfill = """<script>""" +
        """(function(){""" +
        """if(typeof globalThis==='undefined'){globalThis=window;}""" +
        """if(typeof queueMicrotask==='undefined'){queueMicrotask=function(f){Promise.resolve().then(f);};}""" +
        """if(!Promise.allSettled){Promise.allSettled=function(ps){return Promise.all(ps.map(function(p){return Promise.resolve(p).then(function(v){return{status:'fulfilled',value:v};},function(e){return{status:'rejected',reason:e};});}));};}""" +
        """if(!Object.fromEntries){Object.fromEntries=function(it){var o={};for(var e of it){o[e[0]]=e[1];}return o;};}""" +
        """})();""" +
        """</script>"""