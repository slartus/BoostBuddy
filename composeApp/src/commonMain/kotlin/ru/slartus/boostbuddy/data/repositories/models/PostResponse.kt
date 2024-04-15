package ru.slartus.boostbuddy.data.repositories.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
internal data class PostResponse(
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
        val teaser: List<Teaser>? = null,
        val count:PostCount? = null
    )

    @Serializable
    data class PostCount(
        val likes: Int? = null,
        val comments: Int? = null
    )

    @Serializable
    data class Teaser(
        val url: String? = null,
        val type: String? = null,//image
    )

    @Serializable
    data class Extra(
        val offset: String? = null,
        val isLast: Boolean? = null
    )
}