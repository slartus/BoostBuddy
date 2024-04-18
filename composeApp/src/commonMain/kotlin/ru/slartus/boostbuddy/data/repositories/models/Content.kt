package ru.slartus.boostbuddy.data.repositories.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import io.ktor.http.Url
import kotlinx.serialization.Serializable

@Serializable
@Immutable
sealed class Content {
    data class Text(
        val content: PostDataTextContent?,
        val modificator: String?
    ) : Content()

    data class AnnotatedText(
        val content: AnnotatedString,
        val smiles: List<Smile> = emptyList()
    ) : Content()

    @Serializable
    data class OkVideo(
        val vid: String,
        val title: String,
        val playerUrls: List<PlayerUrl>,
        val previewUrl: String,
    ) : Content()

    data class Image(
        val url: String
    ) : Content()

    data class Link(
        val content: PostDataTextContent?,
        val url: String,
        val modificator: String?
    ) : Content() {
        val text: String = content?.text.orEmpty()
    }

    data class Video(
        val url: String
    ) : Content() {
        private val uri: Url? = runCatching { Url(url) }.getOrNull()
        private val isYoutube: Boolean =
            uri?.host?.equals("www.youtube.com", ignoreCase = true) == true
        val previewUrl: String? =
            if (isYoutube && uri != null) getYoutubePreviewUrl(uri.parameters["v"]) else null

        companion object {
            private fun getYoutubePreviewUrl(youtubeId: String?): String =
                "https://img.youtube.com/vi/$youtubeId/maxresdefault.jpg"
        }
    }

    data class AudioFile(
        val title: String,
        val url: String,
        val duration: Long
    ) : Content()

    data class Smile(
        val name: String,
        val largeUrl: String?,
        val smallUrl: String?,
        val mediumUrl: String?
    ) : Content()

    data class File(
        val title: String,
        val url: String,
        val size: Long?
    ) : Content()

    data object Unknown : Content()
}