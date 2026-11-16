package ru.slartus.boostbuddy.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.concurrent.ConcurrentHashMap

internal const val EXTRA_DOWNLOAD_ID = "extra_download_id"

internal object ActiveDownloads {
    private val cancels = ConcurrentHashMap<Int, (Context) -> Unit>()

    fun register(id: Int, cancel: (Context) -> Unit) {
        cancels[id] = cancel
    }

    fun cancel(id: Int, context: Context) {
        cancels.remove(id)?.invoke(context)
    }

    fun done(id: Int) {
        cancels.remove(id)
    }
}

class DownloadCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_DOWNLOAD_ID, -1)
        if (id == -1) return
        ActiveDownloads.cancel(id, context)
    }
}
