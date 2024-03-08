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
        @SerialName("int_id") val intId: Long? = null,
        val data: List<PostData>? = null,
        val user: PostUser? = null,
        val teaser: List<Teaser>? = null
    )


    @Serializable
    data class Teaser(
        val url: String? = null,
        val type: String? = null,//image
    )

    @Serializable
    data class PostData(
        val id: String? = null,
        val vid: String? = null,
        val title: String? = null,
        val type: String? = null,//image, text, link, ok_video
        val url: String? = null,
        val preview: String? = null,
        val defaultPreview: String? = null,
        val playerUrls: List<PlayerUrl>? = null,
        val content: String? = null,
        val modificator: String? = null
    )

    @Serializable
    data class PostUser(
        val name: String? = null,
        val avatarUrl: String? = null
    )

    @Serializable
    data class PlayerUrl(
        val type: String? = null,// full_hd, high, medium, lowest, hls, ultra_hd, dash, low, tiny,
        val url: String? = null
    )


    @Serializable
    data class Extra(
        val offset: String? = null,
        val isLast: Boolean? = null
    )
}