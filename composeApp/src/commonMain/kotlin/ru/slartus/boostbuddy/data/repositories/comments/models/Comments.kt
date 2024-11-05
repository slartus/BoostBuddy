package ru.slartus.boostbuddy.data.repositories.comments.models

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDateTime
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.User
import ru.slartus.boostbuddy.utils.toHumanString

data class Comments(
    val comments: List<Comment>,
    val hasMore: Boolean
)

@Immutable
data class Comment(
    val id: String,
    val intId: Int,
    val author: User,
    val content: List<Content>,
    val replies: Comments,
    val replyCount: Int,
    val replyToUser: User?,
    val createdAt: LocalDateTime
) {
    val createdAtText: String = createdAt.toHumanString()
}