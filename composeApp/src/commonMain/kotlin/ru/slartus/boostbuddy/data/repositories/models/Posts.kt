package ru.slartus.boostbuddy.data.repositories.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable


data class Offset(
    val postId: Long,
    val createdAt: Long
)

data class Posts(
    val items: List<Post>,
    val isLast: Boolean
)

@Immutable
data class Post(
    val id: String,
    val createdAt: Long,
    val intId: Long,
    val title: String,
    val data: List<PostData>,
    val user: PostUser
)

data class PostUser(
    val name: String,
    val avatarUrl: String?
)

@Serializable
@Immutable
sealed class PostData {
    data class Text(
        val rawContent: String,
        val modificator: String
    ) : PostData()

    @Serializable
    data class Video(
        val vid: String,
        val title: String,
        val playerUrls: List<PlayerUrl>,
        val previewUrl: String,
    ) : PostData()

    data object Unknown : PostData()
}

@Serializable
data class PlayerUrl(
    val type: String,
    val url: String
)