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
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.parse(url), mimeType)
        intent.putExtra("title", title)
        intent.putExtra("poster", posterUrl)
        return Intent.createChooser(intent, "")
    }
}