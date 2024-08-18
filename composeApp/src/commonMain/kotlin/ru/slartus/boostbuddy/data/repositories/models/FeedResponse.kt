package ru.slartus.boostbuddy.data.repositories.models

import kotlinx.serialization.Serializable

@Serializable
internal data class FeedResponse(
    val data: Data? = null,
    val extra: PostResponse.Extra? = null
){
    @Serializable
    data class Data(
        val posts: List<PostResponse.Post>? = null,
    )
}