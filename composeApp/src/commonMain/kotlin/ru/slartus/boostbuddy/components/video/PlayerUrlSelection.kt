package ru.slartus.boostbuddy.components.video

import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.VideoQuality

// HLS/DASH идут раньше любого Q*P: адаптивные стримы дают лучший опыт по умолчанию,
// чем фиксированный битрейт.
private val VIDEO_QUALITY_FALLBACK_PRIORITY: List<VideoQuality> = listOf(
    VideoQuality.HLS,
    VideoQuality.DASH,
    VideoQuality.Q4320P,
    VideoQuality.Q2160P,
    VideoQuality.Q1440P,
    VideoQuality.Q1080P,
    VideoQuality.Q720P,
    VideoQuality.Q480P,
    VideoQuality.Q360P,
    VideoQuality.Q240P,
    VideoQuality.Q144P,
)

internal fun List<PlayerUrl>.usableOptions(): List<PlayerUrl> =
    filter { it.url.isNotEmpty() && it.quality.used }

internal fun List<PlayerUrl>.pickPlayerUrl(preferred: VideoQuality?): PlayerUrl? {
    val available = usableOptions()
    if (available.isEmpty()) return null

    if (preferred != null) {
        available.firstOrNull { it.quality == preferred }?.let { return it }
    }
    for (quality in VIDEO_QUALITY_FALLBACK_PRIORITY) {
        available.firstOrNull { it.quality == quality }?.let { return it }
    }
    return available.firstOrNull()
}
