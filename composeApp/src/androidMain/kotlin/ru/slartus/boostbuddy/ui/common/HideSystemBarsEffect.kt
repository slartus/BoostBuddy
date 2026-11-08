package ru.slartus.boostbuddy.ui.common

import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
actual fun HideSystemBarsEffect() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        val previousCutoutMode = window?.drawIntoDisplayCutout()
        if (window != null) {
            val windowInsetsController =
                WindowCompat.getInsetsController(window, window.decorView)
            // Configure the behavior of the hidden system bars.
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (window != null) {
                val windowInsetsController =
                    WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                previousCutoutMode?.let(window::setDisplayCutoutMode)
            }
        }
    }
}

private fun Window.drawIntoDisplayCutout(): Int? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    val previousMode = attributes.layoutInDisplayCutoutMode
    setDisplayCutoutMode(WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES)
    return previousMode
}

private fun Window.setDisplayCutoutMode(mode: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
    attributes = attributes.apply { layoutInDisplayCutoutMode = mode }
}
