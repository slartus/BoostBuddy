package ru.slartus.boostbuddy.data.repositories.comments.models

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.data.repositories.models.ContentResponse
import ru.slartus.boostbuddy.data.repositories.models.UserResponse
import ru.slartus.boostbuddy.data.repositories.models.mapToContentOrNull
import ru.slartus.boostbuddy.data.repositories.models.mapToUserOrNull
import ru.slartus.boostbuddy.data.repositories.models.mergeText

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
        val replyCount: Int? = null,
        val replyToUser: UserResponse? = null,
        val createdAt: Long? = null
    )

    @Serializable
    internal data class Extra(val isLast: Boolean? = null, val isFirst: Boolean? = null)
}

internal fun CommentsResponse.mapToComments(): List<Comment> {
    return data.orEmpty().mapNotNull { it.mapToCommentOrNull() }
}

internal fun CommentsResponse.Comment.mapToCommentOrNull(): Comment? {
    return Comment(
        id = id ?: return null,
        author = author?.mapToUserOrNull() ?: return null,
        createdAt = createdAt?.let {
            Instant.fromEpochMilliseconds(it * 1000)
                .toLocalDateTime(TimeZone.currentSystemDefault())
        } ?: return null,
        content = data?.mapNotNull { it.mapToContentOrNull() }.orEmpty().mergeText(),
        replyCount = replyCount ?: 0,
        replies = replies?.mapToComments().orEmpty(),
        replyToUser = replyToUser?.mapToUserOrNull()
    )
}