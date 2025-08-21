package ru.slartus.boostbuddy.data.repositories.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import ru.slartus.boostbuddy.utils.dateTimeFromUnix
import ru.slartus.boostbuddy.utils.toHumanString
@Serializable
data class Posts(
    val items: List<Post>,
    val extra: Extra?,
)
@Serializable
data class Extra(
    val offset: String,
    val isLast: Boolean
)

@Immutable
@Serializable
data class Post(
    val id: String,
    val createdAt: Long,
    val signedQuery: String,
    val intId: Long,
    val title: String,
    val data: List<Content>,
    val teaser: List<Content>,
    val user: User,
    val count: PostCount,
    val poll: Poll?,
    val hasAccess: Boolean
) {
    val createdAtString: String = dateTimeFromUnix(createdAt).toHumanString()
}

@Immutable
@Serializable
data class Poll(
    val id: Int,
    val title: List<String>,
    val isMultiple: Boolean,
    val isFinished: Boolean,
    val options: List<PollOption>,
    val counter: Int,
    val answer: List<Int>,
    val checked: Set<Int> = emptySet()
) {
    val titleText: String = title.joinToString()
}

@Immutable
@Serializable
data class PollOption(
    val id: Int,
    val text: String,
    val counter: Int,
    val fraction: Int
) {
    val fractionText: String = "$fraction%"
}

@Serializable
data class PostCount(val likes: Int, val comments: Int)
@Serializable
data class PostDataTextContent(
    val text: String,
    val styleData: List<StyleData>?,
    val urls: List<UrlData>?
) {
    companion object {
        fun ofRaw(rawContent: String): PostDataTextContent? = runCatching {
            if (rawContent.isEmpty()) return null
            val x = Json.parseToJsonElement(rawContent) as? JsonArray ?: return null

            val text = x.firstOrNull()?.jsonPrimitive?.content.orEmpty()

            val styleData =
                (x.getOrNull(2) as? JsonArray)
                    ?.map { styleRaw -> (styleRaw as? JsonArray)?.map { it.jsonPrimitive.content } }
                    ?.mapNotNull { StyleData.ofRaw(it) }

            return PostDataTextContent(text, styleData, null)
        }.getOrDefault(PostDataTextContent(rawContent, null, null))
    }
    @Serializable
    data class StyleData(val style: Style, val from: Int, val length: Int) {
        companion object {
            fun ofRaw(styleRaw: List<String>?): StyleData? {
                styleRaw ?: return null
                if (styleRaw.size != 3) return null
                val style = when (styleRaw[0]) {
                    "4" -> Style.Underline
                    "2" -> Style.Italic
                    "0" -> Style.Bold
                    else -> Style.Normal
                }
                val from = styleRaw[1].toIntOrNull() ?: return null
                val length = styleRaw[2].toIntOrNull() ?: return null
                return StyleData(style, from, length)
            }
        }
    }
    @Serializable
    data class UrlData(val url: String, val from: Int, val length: Int)
    enum class Style {
        Normal, Italic, Bold, Underline
    }
}

@Serializable
@Immutable
data class PlayerUrl(
    val quality: VideoQuality,
    val url: String
)

enum class VideoQuality(val used: Boolean) {
    Q144P(true),
    Q240P(true),
    Q360P(true),
    Q480P(true),
    Q720P(true),
    Q1080P(true),
    Q1440P(true),
    Q2160P(true),
    Q4320P(true),

    DASH(true),
    HLS(true),
    MP4(false),
    WEBM(false),
    AV1(false),
    WEBRTC(false),
    RTMP(false),
    LIVE_CMAF(false),
    UNKNOWN(false);

    companion object {
        fun of(string: String?): VideoQuality = when (string?.lowercase()) {
            "tiny",
            "mobile" -> Q144P

            "lowest" -> Q240P
            "low" -> Q360P
            "sd",
            "medium" -> Q480P

            "hd",
            "high" -> Q720P

            "full",
            "full_hd",
            "fullhd" -> Q1080P

            "quad",
            "quad_hd",
            "quadhd" -> Q1440P

            "ultra",
            "ultra_hd",
            "ultrahd" -> Q2160P

            "mp4" -> MP4

            "live_dash",
            "live_playback_dash",
            "dash_uni",
            "dash_sep",
            "ondemand_dash",
            "ondemand_dash_live",
            "dash" -> DASH

            "live_hls",
            "live_playback_hls",
            "live_ondemand_hls",
            "ondemand_hls_live",
            "ondemand_hls",
            "hls" -> HLS

            "webm" -> WEBM
            "av1" -> AV1
            "webrtc" -> WEBRTC
            "rtmp" -> RTMP
            "live_cmaf" -> LIVE_CMAF

            else -> UNKNOWN
        }
    }
}