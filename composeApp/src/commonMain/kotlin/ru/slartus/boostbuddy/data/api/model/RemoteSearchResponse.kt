package ru.slartus.boostbuddy.data.api.model

import kotlinx.serialization.Serializable

@Serializable
internal class RemoteSearchResponse(
    val data: Data? = null,
    val extra: RemotePostResponse.Extra? = null
){
    @Serializable
    class Data(
        val searchPosts: List<SearchPost>? = null,
    )

    @Serializable
    class SearchPost(
        val post: RemotePostResponse.Post? = null
    )
}