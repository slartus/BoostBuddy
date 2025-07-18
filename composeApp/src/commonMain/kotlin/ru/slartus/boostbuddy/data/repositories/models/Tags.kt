package ru.slartus.boostbuddy.data.repositories.models

class Tags(
    val data: Data,
    val extra: Extra?
) {
    class Data(
        val searchTags: List<SearchTag>
    )

    class SearchTag(
        val tag: Tag,
        val rank: Int,
    )

    class Tag(
        val id: String,
        val title: String,
    )
}