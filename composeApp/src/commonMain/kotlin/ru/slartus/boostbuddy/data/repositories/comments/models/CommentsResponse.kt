package ru.slartus.boostbuddy.data.repositories.comments.models

import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.data.repositories.models.ContentResponse
import ru.slartus.boostbuddy.data.repositories.models.UserResponse

@Serializable
internal data class CommentsResponse(
    val data: List<Comment>? = null,
    val extra: Extra? = null
) {

    @Serializable
    internal data class Comment(
        val id: String? = null,
        val data: List<ContentResponse>? = null,
        val author: UserResponse? = null,
        val replies: CommentsResponse? = null,
        val replyCount: Int? = null
    )

    @Serializable
    internal data class Extra(val isLast: Boolean? = null, val isFirst: Boolean? = null)
}