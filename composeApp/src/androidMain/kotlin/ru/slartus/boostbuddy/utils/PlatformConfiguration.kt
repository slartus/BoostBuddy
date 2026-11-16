package ru.slartus.boostbuddy.utils

import android.app.DownloadManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import ru.slartus.boostbuddy.BuildConfig
import ru.slartus.boostbuddy.R
import ru.slartus.boostbuddy.data.log.logger
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

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
        lateinit var job: Job
        var connection: HttpURLConnection? = null
        var uri: Uri? = null
        job = CoroutineScope(Dispatchers.IO).launch {
            val notificationId = System.currentTimeMillis().toInt()
            ActiveDownloads.register(notificationId) { ctx ->
                connection?.disconnect()
                job.cancel()
                uri?.let { u -> runCatching { ctx.contentResolver.delete(u, null, null) } }
                NotificationManagerCompat.from(ctx).cancel(notificationId)
                Toast.makeText(ctx, "Загрузка отменена", Toast.LENGTH_SHORT).show()
            }
            val notificationManager = NotificationManagerCompat.from(androidContext)
            createDownloadChannel()
            val notificationsEnabled = notificationManager.areNotificationsEnabled()
            val showNotification: (Notification) -> Unit = { notification ->
                if (notificationsEnabled) {
                    runCatching { notificationManager.notify(notificationId, notification) }
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(androidContext, "Загрузка началась", Toast.LENGTH_SHORT).show()
            }
            if (notificationsEnabled) {
                showNotification(buildProgressNotification(notificationId, fileName, 0, indeterminate = true))
            }
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                if (mimeType != null) {
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                }
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            try {
                val targetUri = androidContext.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                ) ?: throw IOException("Failed to create MediaStore entry")
                uri = targetUri
                val httpConnection = URL(url).openConnection() as HttpURLConnection
                connection = httpConnection
                try {
                    httpConnection.requestMethod = "GET"
                    httpConnection.connectTimeout = CONNECT_TIMEOUT_MS
                    httpConnection.readTimeout = READ_TIMEOUT_MS
                    httpConnection.setRequestProperty("User-Agent", BROWSER_USER_AGENT)
                    httpConnection.setRequestProperty("Accept", "*/*")
                    httpConnection.setRequestProperty("Range", "bytes=0-")
                    httpConnection.setRequestProperty("Accept-Encoding", "identity")
                    val responseCode = httpConnection.responseCode
                    if (responseCode !in 200..299) {
                        throw IOException("HTTP $responseCode for $url")
                    }
                    logger.d("downloadFile started code=$responseCode length=${httpConnection.contentLength} url=$url")
                    val totalBytes = httpConnection.contentLength.toLong()
                    var downloadedBytes = 0L
                    var lastNotificationUpdate = 0L
                    val startTime = SystemClock.elapsedRealtime()
                    var lastLogTime = 0L
                    BufferedInputStream(httpConnection.inputStream, BUFFER_SIZE).use { input ->
                        androidContext.contentResolver.openOutputStream(targetUri).use { output ->
                            requireNotNull(output)
                            val buffer = ByteArray(BUFFER_SIZE)
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                downloadedBytes += read
                                val now = SystemClock.elapsedRealtime()
                                if (now - lastNotificationUpdate >= NOTIFICATION_UPDATE_INTERVAL_MS) {
                                    lastNotificationUpdate = now
                                    val indeterminate = totalBytes <= 0
                                    val percent =
                                        if (indeterminate) 0
                                        else (downloadedBytes * 100 / totalBytes).toInt()
                                    showNotification(
                                        buildProgressNotification(
                                            notificationId,
                                            fileName,
                                            percent,
                                            indeterminate
                                        )
                                    )
                                }
                                if (now - lastLogTime >= LOG_INTERVAL_MS) {
                                    lastLogTime = now
                                    val percent =
                                        if (totalBytes > 0) downloadedBytes * 100 / totalBytes else 0
                                    logger.d(
                                        "downloadFile progress downloaded=$downloadedBytes " +
                                            "total=$totalBytes percent=$percent " +
                                            "speed=${formatSpeed(downloadedBytes, now - startTime)}MB/s"
                                    )
                                }
                            }
                        }
                    }
                    val elapsed = SystemClock.elapsedRealtime() - startTime
                    logger.d(
                        "downloadFile completed bytes=$downloadedBytes elapsed=${elapsed}ms " +
                            "avgSpeed=${formatSpeed(downloadedBytes, elapsed)}MB/s"
                    )
                } finally {
                    httpConnection.disconnect()
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                androidContext.contentResolver.update(targetUri, values, null, null)
                ActiveDownloads.done(notificationId)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        androidContext,
                        "Файл сохранён в Downloads",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                if (notificationsEnabled) {
                    showNotification(buildCompletedNotification(fileName))
                }
            } catch (error: Exception) {
                ActiveDownloads.done(notificationId)
                uri?.let { targetUri ->
                    runCatching { androidContext.contentResolver.delete(targetUri, null, null) }
                }
                notificationManager.cancel(notificationId)
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

    private fun createDownloadChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                "Загрузки",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager =
                androidContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildProgressNotification(
        notificationId: Int,
        fileName: String,
        percent: Int,
        indeterminate: Boolean
    ): Notification {
        val cancelIntent = Intent(androidContext, DownloadCancelReceiver::class.java).apply {
            putExtra(EXTRA_DOWNLOAD_ID, notificationId)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            androidContext,
            notificationId,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(androidContext, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(fileName)
            .setContentText(if (indeterminate) "Загрузка…" else "Загрузка: $percent%")
            .setProgress(100, percent, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "Отмена", cancelPendingIntent)
            .build()
    }

    private fun buildCompletedNotification(fileName: String): Notification =
        NotificationCompat.Builder(androidContext, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(fileName)
            .setContentText("Файл сохранён в Downloads")
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

    private fun formatSpeed(bytes: Long, elapsedMs: Long): String {
        val elapsedSeconds = elapsedMs / 1000.0
        val speed = if (elapsedSeconds > 0) bytes / elapsedSeconds / 1024 / 1024 else 0.0
        return String.format(Locale.US, "%.2f", speed)
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
        private const val READ_TIMEOUT_MS = 60_000
        private const val BUFFER_SIZE = 64 * 1024
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 500L
        private const val LOG_INTERVAL_MS = 3000L
        private const val DOWNLOAD_CHANNEL_ID = "downloads"
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