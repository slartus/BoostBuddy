package ru.slartus.boostbuddy.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import ru.slartus.boostbuddy.BuildConfig

actual class PlatformConfiguration(var androidContext: Context, actual val platform: Platform) {
    actual val appVersion: String = BuildConfig.VERSION_NAME
    actual val isDebug: Boolean = BuildConfig.DEBUG

    actual fun openBrowser(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        androidContext.tryStartActivity(browserIntent)
    }

    companion object {
        private fun Context.tryStartActivity(intent: Intent) {
            runCatching {
                startActivity(intent)
            }.onFailure { error ->
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