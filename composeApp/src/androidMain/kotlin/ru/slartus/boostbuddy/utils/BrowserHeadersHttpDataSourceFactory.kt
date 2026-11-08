package ru.slartus.boostbuddy.utils

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import ru.slartus.boostbuddy.data.ktor.DESKTOP_USER_AGENT

private const val BOOSTY_ORIGIN = "https://boosty.to"
private const val BOOSTY_REFERER = "https://boosty.to/"

@UnstableApi
internal fun browserHeadersHttpDataSourceFactory(): DefaultHttpDataSource.Factory =
    DefaultHttpDataSource.Factory()
        .setUserAgent(DESKTOP_USER_AGENT)
        .setDefaultRequestProperties(
            mapOf(
                "Origin" to BOOSTY_ORIGIN,
                "Referer" to BOOSTY_REFERER,
            )
        )
