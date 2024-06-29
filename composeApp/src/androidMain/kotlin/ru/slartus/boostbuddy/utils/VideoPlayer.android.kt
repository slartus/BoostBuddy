package ru.slartus.boostbuddy.utils

import android.content.Intent
import android.net.Uri

actual class VideoPlayer {
    actual fun playUrl(platformConfiguration: PlatformConfiguration, title: String, url: String, mimeType: String?, posterUrl: String?) {
        val playerIntent = getPlayerIntent(title, url, mimeType, posterUrl)
        playerIntent.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            platformConfiguration.androidContext.startActivity(this)
        }
    }

    private fun getPlayerIntent(title: String, url: String, mimeType: String?, posterUrl: String?): Intent {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), mimeType)
            putExtra("title", title)
            putExtra("poster", posterUrl)
            putMxPlayerExtra(title)
            putVimuPlayerExtra(title)
        }

        return Intent.createChooser(intent, "")
    }

    private fun Intent.putMxPlayerExtra(title: String) {
        putExtra("title",title)
        putExtra("sticky", false)
    }

    private fun Intent.putVimuPlayerExtra(title: String) {
        putExtra("forcename", title)
        putExtra("forcedirect", true)
        putExtra("forceresume", true)
    }
}