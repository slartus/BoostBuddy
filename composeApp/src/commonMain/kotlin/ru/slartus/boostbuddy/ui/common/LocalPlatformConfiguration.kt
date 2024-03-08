package ru.slartus.boostbuddy.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import ru.slartus.boostbuddy.utils.PlatformConfiguration

val LocalPlatformConfiguration = staticCompositionLocalOf<PlatformConfiguration> {
    error("No platform configuration provided")
}