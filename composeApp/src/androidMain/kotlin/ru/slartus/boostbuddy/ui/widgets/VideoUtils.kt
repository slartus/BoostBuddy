package ru.slartus.boostbuddy.ui.widgets

import android.os.SystemClock
import java.util.Locale
import kotlin.math.max

internal class SeekState {
    private var seekStartMs: Long = 0

    fun calcSeekMultiplier(longPress: Boolean): Float {
        if (!longPress) {
            seekStartMs = 0
            return 1f
        }

        if (seekStartMs == 0L) {
            seekStartMs = SystemClock.uptimeMillis()
        }

        return max((SystemClock.uptimeMillis() - seekStartMs) / 1000f, 1f)
    }
}

internal fun formatDuration(durationMs: Long): String {
    val hours = durationMs / 3_600_000
    val minutes = (durationMs % 3_600_000) / 60_000
    val seconds = (durationMs % 60_000) / 1_000

    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}
