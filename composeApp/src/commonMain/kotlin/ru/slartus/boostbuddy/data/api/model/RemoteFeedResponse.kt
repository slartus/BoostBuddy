package ru.slartus.boostbuddy.data.api.model

import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.data.repositories.models.PostResponse

@Serializable
internal data class RemoteFeedResponse(
    val data: Data? = null,
    val extra: PostResponse.Extra? = null
){
    @Serializable
    data class Data(
        val posts: List<PostResponse.Post>? = null,
    )
}