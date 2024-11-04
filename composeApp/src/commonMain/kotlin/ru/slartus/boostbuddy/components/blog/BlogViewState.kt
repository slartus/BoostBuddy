package ru.slartus.boostbuddy.components.blog

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.slartus.boostbuddy.components.common.ProgressState
import ru.slartus.boostbuddy.components.feed.FeedPostItem
import ru.slartus.boostbuddy.data.repositories.Blog
import ru.slartus.boostbuddy.data.repositories.models.Extra
import ru.slartus.boostbuddy.data.repositories.models.VideoQuality

data class BlogViewState(
    val blog: Blog,
    val items: ImmutableList<FeedPostItem> = persistentListOf(),
    val extra: Extra? = null,
    val progressState: ProgressState = ProgressState.Init,
) {
    val hasMore: Boolean = extra?.isLast == false
}

val VideoQuality.text
    get() = when (this) {
        VideoQuality.Q144P -> "144p"
        VideoQuality.Q240P -> "240p"
        VideoQuality.Q360P -> "360p"
        VideoQuality.Q480P -> "480p"
        VideoQuality.Q720P -> "720p"
        VideoQuality.Q1080P -> "1080p"
        VideoQuality.Q1440P -> "1440p"
        VideoQuality.Q2160P -> "2160p"
        VideoQuality.Q4320P -> "4320p"
        VideoQuality.HLS -> "HTTP Live Streaming"
        VideoQuality.DASH -> "Dynamic Adaptive Streaming over HTTP"
        VideoQuality.MP4,
        VideoQuality.WEBM,
        VideoQuality.AV1,
        VideoQuality.WEBRTC,
        VideoQuality.UNKNOWN,
        VideoQuality.LIVE_CMAF,
        VideoQuality.RTMP -> "UNKNOWN"
    }