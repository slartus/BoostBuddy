package ru.slartus.boostbuddy.utils

import android.content.Context

actual class PlatformConfiguration constructor(
    var androidContext: Context,
    actual val platform: Platform = Platform.Android
)