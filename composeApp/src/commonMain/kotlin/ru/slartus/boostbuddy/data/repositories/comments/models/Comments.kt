package ru.slartus.boostbuddy.data.repositories.comments.models

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
    val createdAt: LocalDateTime
) {
    val createdAtText: String = buildString {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        append("${createdAt.dayOfMonth} ${createdAt.month.name.lowercase()}")
        if (now.year != createdAt.year)
            append(" ${createdAt.year}")
        append(" ${createdAt.hour}:${createdAt.minute.toString().padStart(2, '0')}")
    }
}