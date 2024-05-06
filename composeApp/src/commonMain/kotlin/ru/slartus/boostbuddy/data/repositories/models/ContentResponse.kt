package ru.slartus.boostbuddy.data.repositories.models

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import kotlinx.serialization.Serializable

@Serializable
internal data class ContentResponse(
    val id: String? = null,
    val vid: String? = null,
    val title: String? = null,
    val type: String? = null,//image, text, link, ok_video
    val fileType: String? = null,// MP3
    val url: String? = null,
    val preview: String? = null,
    val defaultPreview: String? = null,
    val playerUrls: List<PlayerUrl>? = null,
    val content: String? = null,
    val modificator: String? = null,
    val name: String? = null,
    val largeUrl: String? = null,
    val mediumUrl: String? = null,
    val smallUrl: String? = null,
    val duration: Long? = null,
    val size: Long? = null,
    val timeCode: Long? = null
) {
    @Serializable
    data class PlayerUrl(
        val type: String? = null,// full_hd, high, medium, lowest, hls, ultra_hd, dash, low, tiny,
        val url: String? = null
    )
}

private val playerUrlsComparator = object : Comparator<PlayerUrl> {
    private val sortedQualities =
        listOf(
            VideoQuality.HLS,
            VideoQuality.DASH,
            VideoQuality.Q144P,
            VideoQuality.Q240P,
            VideoQuality.Q360P,
            VideoQuality.Q480P,
            VideoQuality.Q720P,
            VideoQuality.Q1080P,
            VideoQuality.Q1440P,
            VideoQuality.Q2160P,
            VideoQuality.Q4320P
        )
            .reversed()

    override fun compare(a: PlayerUrl, b: PlayerUrl): Int =
        sortedQualities.indexOf(a.quality).compareTo(sortedQualities.indexOf(b.quality))
}

internal fun ContentResponse.mapToContentOrNull(): Content? {
    return when (type) {
        "ok_video" -> Content.OkVideo(
            id = id ?: return null,
            vid = vid ?: return null,
            title = title.orEmpty(),
            playerUrls = playerUrls?.mapNotNull { it.mapToPlayerUrlOrNull() }.orEmpty()
                .filter { it.quality.used }
                .sortedWith(playerUrlsComparator)
                .ifEmpty { return null },
            previewUrl = preview ?: defaultPreview ?: return null,
            timeCode = timeCode ?: 0
        )

        "video" -> Content.Video(
            url = url ?: return null,
        )

        "text" -> {
            Content.Text(
                content = PostDataTextContent.ofRaw(content.orEmpty()),
                modificator = modificator.orEmpty(),
            )
        }

        "image" -> Content.Image(
            url = url ?: return null,
        )

        "link" -> Content.Link(
            content = PostDataTextContent.ofRaw(content.orEmpty()),
            url = url ?: return null,
            modificator = modificator
        )

        "audio_file" -> Content.AudioFile(
            title = title.orEmpty(),
            url = url ?: return null,
            duration = duration ?: return null,
        )

        "smile" -> Content.Smile(
            name = name.orEmpty(),
            largeUrl = largeUrl,
            mediumUrl = mediumUrl,
            smallUrl = smallUrl
        )

        "file" -> Content.File(
            title = title.orEmpty(),
            url = url ?: return null,
            size = size
        )

        else -> Content.Unknown
    }
}

private fun ContentResponse.PlayerUrl.mapToPlayerUrlOrNull(): PlayerUrl? {
    return PlayerUrl(VideoQuality.of(type), url ?: return null)
}

val linkColor = Color(241, 95, 44)
private fun PostDataTextContent.Style.toSpanStyle(): SpanStyle = when (this) {
    PostDataTextContent.Style.Normal -> SpanStyle(fontStyle = FontStyle.Normal)
    PostDataTextContent.Style.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
    PostDataTextContent.Style.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
    PostDataTextContent.Style.Underline -> SpanStyle(textDecoration = TextDecoration.Underline)
}

private fun AnnotatedString.Builder.append(content: PostDataTextContent) {
    append(content.text)
    content.styleData?.let { items ->
        items.forEach { styleData ->
            addStyle(
                styleData.style.toSpanStyle(),
                styleData.from,
                styleData.from + styleData.length
            )
        }
    }
    content.urls?.let { items ->
        items.forEach { url ->
            addStringAnnotation(
                tag = "URL",
                annotation = url.url,
                start = url.from,
                end = url.from + url.length
            )
            addStyle(
                SpanStyle(color = linkColor),
                start = url.from,
                end = url.from + url.length
            )
        }
    }
}

internal fun List<Content>.mergeText(): List<Content> {
    val result: MutableList<Content> = mutableListOf()
    var mergeContainer: Content.AnnotatedText? = null
    forEach { item ->
        when (item) {
            is Content.Smile -> {
                mergeContainer.let { container ->
                    mergeContainer = container?.copy(
                        content = buildAnnotatedString {
                            append(container.content)
                            appendInlineContent(id = item.name)
                        },
                        smiles = container.smiles + item
                    )
                        ?: Content.AnnotatedText(
                            content = buildAnnotatedString {
                                appendInlineContent(id = item.name)
                            },
                            smiles = listOf(item)
                        )
                }
            }

            is Content.Link -> {
                mergeContainer.let { container ->
                    if (container != null && item.modificator != "BLOCK_END") {
                        mergeContainer = container.copy(
                            content = buildAnnotatedString {
                                append(container.content)
                                item.content?.let {
                                    append(
                                        it.copy(
                                            urls = listOf(
                                                PostDataTextContent.UrlData(
                                                    item.url,
                                                    container.content.text.length,
                                                    item.content.text.length
                                                )
                                            )
                                        )
                                    )
                                }
                            }
                        )
                    } else if (item.modificator != "BLOCK_END") {
                        mergeContainer = Content.AnnotatedText(buildAnnotatedString {
                            item.content?.let {
                                append(
                                    it.copy(
                                        urls = listOf(
                                            PostDataTextContent.UrlData(
                                                item.url,
                                                0,
                                                item.content.text.length
                                            )
                                        )
                                    )
                                )
                            }
                        })
                    }
                    if (mergeContainer != null && item.modificator == "BLOCK_END") {
                        result.add(mergeContainer!!)
                        mergeContainer = null
                    }
                }
            }

            is Content.Text -> {
                mergeContainer.let { container ->
                    if (container != null && item.modificator != "BLOCK_END") {
                        mergeContainer = container.copy(
                            content = buildAnnotatedString {
                                append(container.content)
                                item.content?.let { append(it) }
                            }
                        )
                    } else if (item.modificator != "BLOCK_END") {
                        mergeContainer = Content.AnnotatedText(buildAnnotatedString {
                            item.content?.text?.let {
                                append(it)
                            }
                            item.content?.styleData?.let { items ->
                                items.forEach { styleData ->
                                    addStyle(
                                        styleData.style.toSpanStyle(),
                                        styleData.from,
                                        styleData.from + styleData.length
                                    )
                                }
                            }
                            item.content?.urls?.let { items ->
                                items.forEach { url ->
                                    addStringAnnotation(
                                        tag = "URL",
                                        annotation = url.url,
                                        start = url.from,
                                        end = url.from + url.length
                                    )
                                    addStyle(
                                        SpanStyle(color = linkColor),
                                        start = url.from,
                                        end = url.from + url.length
                                    )
                                }
                            }
                        })
                    }
                    if (mergeContainer != null && item.modificator == "BLOCK_END") {
                        result.add(mergeContainer!!)
                        mergeContainer = null
                    }
                }
            }

            else -> {
                if (mergeContainer != null) {
                    result.add(mergeContainer!!)
                    mergeContainer = null
                }
                result.add(item)
            }
        }
    }
    if (mergeContainer != null)
        result.add(mergeContainer!!)

    return result
}