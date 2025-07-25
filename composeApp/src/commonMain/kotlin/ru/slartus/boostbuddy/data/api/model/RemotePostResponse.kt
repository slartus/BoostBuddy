package ru.slartus.boostbuddy.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.data.repositories.models.ContentResponse
import ru.slartus.boostbuddy.data.repositories.models.UserResponse


@Serializable
internal data class RemotePostResponse(
    val data: List<Post>? = null,
    val extra: Extra? = null
) {
    @Serializable
    data class Post(
        val id: String? = null,
        val title: String? = null,
        val createdAt: Long? = null,
        val signedQuery: String? = null,
        @SerialName("int_id") val intId: Long? = null,
        val data: List<ContentResponse>? = null,
        val user: UserResponse? = null,
        val teaser: List<ContentResponse>? = null,
        val count: PostCount? = null,
        val poll: Poll? = null,
        val hasAccess: Boolean? = null
    )

    @Serializable
    data class PostCount(
        val likes: Int? = null,
        val comments: Int? = null
    )

    @Serializable
    data class Extra(
        val offset: String? = null,
        val isLast: Boolean? = null
    )

    @Serializable
    data class Poll(
        val id: Int? = null,
        val title: List<String>? = null,
        val isMultiple: Boolean? = null,
        val isFinished: Boolean? = null,
        val options: List<PollOption>? = null,
        val counter: Int? = null,
        val answer: List<Int>? = null
    )

    @Serializable
    data class PollOption(
        val id: Int? = null,
        val text: String? = null,
        val counter: Int? = null,
        val fraction: Int? = null
    )
}