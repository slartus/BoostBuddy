package ru.slartus.boostbuddy.data.api.model

import kotlinx.serialization.Serializable

@Serializable
class RemoteTagResponse(
    val data: Data? = null,
    val extra: RemoteExtraResponse? = null
) {
    @Serializable
    class Data(
        val searchTags: List<SearchTag>? = null
    )

    @Serializable
    class SearchTag(
        val tag: Tag? = null,
        val rank: Int? = null,
    )

    @Serializable
    class Tag(
        val id: String? = null,
        val title: String? = null,
    )
}