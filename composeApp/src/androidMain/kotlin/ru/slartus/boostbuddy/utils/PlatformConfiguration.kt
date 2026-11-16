package ru.slartus.boostbuddy.utils

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import ru.slartus.boostbuddy.BuildConfig
import ru.slartus.boostbuddy.data.log.logger
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

actual class PlatformConfiguration(var androidContext: Context, actual val platform: Platform) {
    actual val appVersion: String = BuildConfig.VERSION_NAME
    actual val isDebug: Boolean = BuildConfig.DEBUG

    actual fun openBrowser(url: String, onError: (() -> Unit)?) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        androidContext.tryStartActivity(browserIntent, onError)
    }

    actual fun installApp(path: Path) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = uriFromFile(androidContext, File(path.toString()))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        androidContext.startActivity(intent)
    }

    actual fun shareText(text: String, onError: (() -> Unit)?) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        androidContext.tryStartActivity(shareIntent, onError)
    }

    actual fun shareFile(path: Path) {
        val file = File(path.toString())
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uriFromFile(androidContext, file))
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        androidContext.tryStartActivity(shareIntent, null)
    }

    actual fun downloadFile(
        url: String,
        fileName: String,
        mimeType: String?,
        onError: (() -> Unit)?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            downloadToMediaStore(url, fileName, mimeType, onError)
        } else {
            downloadWithDownloadManager(url, fileName, mimeType, onError)
        }
    }

    private fun downloadToMediaStore(
        url: String,
        fileName: String,
        mimeType: String?,
        onError: (() -> Unit)?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(androidContext, "Загрузка началась", Toast.LENGTH_SHORT).show()
            }
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                if (mimeType != null) {
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                }
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            var uri: Uri? = null
            try {
                val targetUri = androidContext.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                ) ?: throw IOException("Failed to create MediaStore entry")
                uri = targetUri
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "GET"
                    connection.connectTimeout = CONNECT_TIMEOUT_MS
                    connection.readTimeout = READ_TIMEOUT_MS
                    connection.setRequestProperty("User-Agent", BROWSER_USER_AGENT)
                    connection.setRequestProperty("Accept", "*/*")
                    val responseCode = connection.responseCode
                    if (responseCode !in 200..299) {
                        throw IOException("HTTP $responseCode for $url")
                    }
                    logger.d("downloadFile started code=$responseCode length=${connection.contentLength} url=$url")
                    connection.inputStream.use { input ->
                        androidContext.contentResolver.openOutputStream(targetUri).use { output ->
                            requireNotNull(output)
                            input.copyTo(output)
                        }
                    }
                } finally {
                    connection.disconnect()
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                androidContext.contentResolver.update(targetUri, values, null, null)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        androidContext,
                        "Файл сохранён в Downloads",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            } catch (error: Exception) {
                uri?.let { targetUri ->
                    runCatching { androidContext.contentResolver.delete(targetUri, null, null) }
                }
                logger.e(error, "downloadFile")
                withContext(Dispatchers.Main) {
                    if (onError != null) {
                        onError.invoke()
                    } else {
                        Toast.makeText(
                            androidContext,
                            "Не удалось запустить загрузку",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            }
        }
    }

    private fun downloadWithDownloadManager(
        url: String,
        fileName: String,
        mimeType: String?,
        onError: (() -> Unit)?
    ) {
        runCatching {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("BoostBuddy")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(
                    androidContext,
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            if (mimeType != null) {
                request.setMimeType(mimeType)
            }
            val manager =
                androidContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
        }.onFailure { error ->
            logger.e(error, "downloadFile")
            if (onError != null) {
                onError.invoke()
            } else {
                Toast.makeText(
                    androidContext,
                    "Не удалось запустить загрузку",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

        fun uriFromFile(context: Context, file: File): Uri {
            return FileProvider.getUriForFile(
                context, BuildConfig.APPLICATION_ID + ".provider",
                file
            )
        }

        private fun Context.tryStartActivity(intent: Intent, onError: (() -> Unit)?) {
            runCatching {
                startActivity(intent)
            }.onFailure { error ->
                logger.e(error,"tryStartActivity")
                if (onError != null) {
                    onError.invoke()
                } else {
                    when (error) {
                        is ActivityNotFoundException -> Toast.makeText(
                            this,
                            "Не найдено приложения для запуска",
                            Toast.LENGTH_LONG
                        ).show()

                        else -> throw error
                    }
                }
            }
        }
    }
}