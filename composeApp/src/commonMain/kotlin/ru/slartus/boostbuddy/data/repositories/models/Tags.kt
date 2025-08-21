package ru.slartus.boostbuddy.data.repositories.models

import kotlinx.serialization.Serializable

@Serializable
class Tags(
    val data: Data,
    val extra: Extra?
) {
    @Serializable
    class Data(
        val searchTags: List<SearchTag>
    )

    @Serializable
    class SearchTag(
        val tag: Tag,
        val rank: Int,
    )

    @Serializable
    class Tag(
        val id: String,
        val title: String,
    )
}