package ru.slartus.boostbuddy.data.repositories.comments.models

import androidx.compose.runtime.Immutable
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.User

@Immutable
data class Comment(
    val id: String,
    val author: User,
    val content: List<Content>,
    val replies: List<Comment>,
    val replyCount: Int,
    val replyToUser: User?,
)