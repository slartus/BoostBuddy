package ru.slartus.boostbuddy.data.api.model

import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.data.repositories.models.PostResponse

@Serializable
internal class RemoteFeedSearchResponse(
    val data: Data? = null,
    val extra: PostResponse.Extra? = null
){
    @Serializable
    class Data(
        val searchPosts: List<SearchPost>? = null,
    )

    @Serializable
    class SearchPost(
        val post: PostResponse.Post? = null
    )
}