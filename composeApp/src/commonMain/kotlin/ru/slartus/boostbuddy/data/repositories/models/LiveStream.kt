package ru.slartus.boostbuddy.data.repositories.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
data class LiveStream(
    val id: String,
    val intId: Long,
    val blogUrl: String,
    val title: String,
    val status: Status,
    val viewersCount: Int,
    val likesCount: Int,
    val isLiked: Boolean,
    val hasAccess: Boolean,
    val signedQuery: String,
    val coverImageUrl: String?,
    val subscription: Subscription?,
    val video: Content.OkVideo?,
) {
    @Serializable
    sealed class Status {
        @Serializable
        data class Live(val startedAt: Long?) : Status()

        @Serializable
        data object Scheduled : Status()
    }

    @Serializable
    @Immutable
    data class Subscription(
        val name: String,
        val priceRub: Double?,
    )
}
