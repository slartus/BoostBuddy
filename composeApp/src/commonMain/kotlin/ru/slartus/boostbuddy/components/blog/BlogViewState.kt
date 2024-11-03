package ru.slartus.boostbuddy.components.blog

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.slartus.boostbuddy.data.repositories.Blog
import ru.slartus.boostbuddy.data.repositories.models.Extra
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.data.repositories.models.VideoQuality

data class BlogViewState(
    val blog: Blog,
    val items: ImmutableList<BlogItem> = persistentListOf(),
    val extra: Extra? = null,
    val progressProgressState: ProgressState = ProgressState.Init,
) {
    val hasMore: Boolean = extra?.isLast == false

    sealed class ProgressState {
        data object Init : ProgressState()
        data object Loading : ProgressState()
        data object Loaded : ProgressState()
        data class Error(val description: String) : ProgressState()
    }
}

@Immutable
sealed class BlogItem(val key: String, val contentType: String) {
    data class PostItem(val post: Post) : BlogItem(post.id, "post")
    data object LoadingItem : BlogItem("loading", "loading")
    data class ErrorItem(val description: String) : BlogItem("error", "error")
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