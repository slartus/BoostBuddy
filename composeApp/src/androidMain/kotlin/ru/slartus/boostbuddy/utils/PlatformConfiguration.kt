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
import kotlinx.io.files.Path
import ru.slartus.boostbuddy.BuildConfig
import ru.slartus.boostbuddy.data.log.logger
import java.io.File

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
        var mediaStoreUri: Uri? = null
        runCatching {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("BoostBuddy")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            if (mimeType != null) {
                request.setMimeType(mimeType)
            }
            // Scoped storage: with targetSdk 29+ the public Downloads dir is only reachable via MediaStore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    if (mimeType != null) {
                        put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    }
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = androidContext.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                )
                if (uri != null) {
                    mediaStoreUri = uri
                    request.setDestinationUri(uri)
                } else {
                    request.setDestinationInExternalFilesDir(
                        androidContext,
                        Environment.DIRECTORY_DOWNLOADS,
                        fileName
                    )
                }
            } else {
                request.setDestinationInExternalFilesDir(
                    androidContext,
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )
            }
            val manager =
                androidContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
        }.onFailure { error ->
            mediaStoreUri?.let { uri ->
                runCatching { androidContext.contentResolver.delete(uri, null, null) }
            }
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