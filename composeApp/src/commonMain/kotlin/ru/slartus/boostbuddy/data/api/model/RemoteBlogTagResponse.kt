package ru.slartus.boostbuddy.data.api.model

import kotlinx.serialization.Serializable

@Serializable
class RemoteBlogTagResponse(
    val data: List<Tag>? = null,
) {
    @Serializable
    class Tag(
        val id: String? = null,
        val title: String? = null,
    )
}