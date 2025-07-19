package ru.slartus.boostbuddy.data.api.model

import kotlinx.serialization.Serializable

@Serializable
internal data class RemoteFeedResponse(
    val data: Data? = null,
    val extra: RemotePostResponse.Extra? = null
){
    @Serializable
    data class Data(
        val posts: List<RemotePostResponse.Post>? = null,
    )
}