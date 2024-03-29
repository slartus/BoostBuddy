package ru.slartus.boostbuddy.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication

@Composable
actual fun KeepScreenOnEffect() {
    DisposableEffect(Unit) {
        UIApplication.sharedApplication.setIdleTimerDisabled(true)
        onDispose {
            UIApplication.sharedApplication.setIdleTimerDisabled(false)
        }
    }
}