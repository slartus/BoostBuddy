package ru.slartus.boostbuddy.data.repositories.models

import kotlinx.serialization.Serializable

@Serializable
internal data class ContentResponse(
    val id: String? = null,
    val vid: String? = null,
    val title: String? = null,
    val type: String? = null,//image, text, link, ok_video
    val url: String? = null,
    val preview: String? = null,
    val defaultPreview: String? = null,
    val playerUrls: List<PlayerUrl>? = null,
    val content: String? = null,
    val modificator: String? = null,
    val name: String? = null,
    val largeUrl: String? = null,
    val mediumUrl: String? = null,
    val smallUrl: String? = null
) {
    @Serializable
    data class PlayerUrl(
        val type: String? = null,// full_hd, high, medium, lowest, hls, ultra_hd, dash, low, tiny,
        val url: String? = null
    )
}

private val playerUrlsComparator = object : Comparator<PlayerUrl> {
    private val sortedQualities =
        listOf("tiny", "lowest", "low", "hls", "medium", "high", "quad_hd", "full_hd")
            .reversed()

    override fun compare(a: PlayerUrl, b: PlayerUrl): Int =
        sortedQualities.indexOf(a.type).compareTo(sortedQualities.indexOf(b.type))
}

internal fun ContentResponse.mapToContentOrNull(): Content? {
    return when (type) {
        "ok_video" -> Content.OkVideo(
            vid = vid ?: return null,
            title = title.orEmpty(),
            playerUrls = playerUrls?.mapNotNull { it.mapToPlayerUrlOrNull() }.orEmpty()
                .sortedWith(playerUrlsComparator)
                .ifEmpty { return null },
            previewUrl = preview ?: defaultPreview ?: return null,
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
        )

        "smile" -> Content.Smile(
            name = name.orEmpty(),
            largeUrl = largeUrl,
            mediumUrl = mediumUrl,
            smallUrl = smallUrl
        )

        else -> Content.Unknown
    }
}

private fun ContentResponse.PlayerUrl.mapToPlayerUrlOrNull(): PlayerUrl? {
    return PlayerUrl(type ?: return null, url ?: return null)
}

internal fun List<Content>.mergeText(): List<Content> {
    val result: MutableList<Content> = mutableListOf()
    var mergeContainer: Content.Text? = null
    forEach { item ->
        when (item) {
            is Content.Link -> {
                mergeContainer.let { container ->
                    if (container != null && item.modificator != "BLOCK_END") {
                        mergeContainer = container.copy(
                            content = container.content?.copy(
                                text = container.content.text + item.content?.text,
                                styleData = container.content.styleData.orEmpty() + item.content?.styleData.orEmpty(),
                                urls = container.content.urls.orEmpty() +
                                        PostDataTextContent.UrlData(
                                            item.url,
                                            container.content.text.length,
                                            item.content?.text?.length ?: 0
                                        )
                            ),
                        )
                    } else if (item.modificator != "BLOCK_END") {
                        mergeContainer = Content.Text(
                            item.content?.copy(
                                urls = listOf(
                                    PostDataTextContent.UrlData(
                                        item.url,
                                        0,
                                        item.content.text.length
                                    )
                                )
                            ), ""
                        )
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
                            content = container.content?.copy(
                                text = container.content.text + item.content?.text,
                                styleData = container.content.styleData.orEmpty() + item.content?.styleData.orEmpty()
                            ),
                        )
                    } else if (item.modificator != "BLOCK_END") {
                        mergeContainer = Content.Text(item.content, "")
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