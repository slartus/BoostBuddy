package ru.slartus.boostbuddy.data.repositories.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive


data class Offset(
    val postId: Long,
    val createdAt: Long
)

data class Posts(
    val items: List<Post>,
    val isLast: Boolean,
)

@Immutable
@Serializable
data class Post(
    val id: String,
    val createdAt: Long,
    val intId: Long,
    val title: String,
    val data: List<Content>,
    val user: User,
    val count: PostCount
)

@Serializable
data class PostCount(val likes: Int, val comments: Int)

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

    data class UrlData(val url: String, val from: Int, val length: Int)
    enum class Style {
        Normal, Italic, Bold, Underline
    }
}

@Serializable
data class PlayerUrl(
    val type: String,
    val url: String
)