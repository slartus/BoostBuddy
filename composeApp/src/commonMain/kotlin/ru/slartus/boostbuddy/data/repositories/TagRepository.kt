package ru.slartus.boostbuddy.data.repositories

import ru.slartus.boostbuddy.data.api.BoostyApi
import ru.slartus.boostbuddy.data.api.model.RemoteExtraResponse
import ru.slartus.boostbuddy.data.api.model.RemoteTagResponse
import ru.slartus.boostbuddy.data.repositories.models.Extra
import ru.slartus.boostbuddy.data.repositories.models.Tags
import ru.slartus.boostbuddy.utils.fetchOrError

internal class TagRepository(
    private val boostyApi: BoostyApi,
) {
    suspend fun getFeedTags(
        limit: Int,
        offset: String?,
    ): Result<Tags> =
        fetchOrError {
            val response: RemoteTagResponse = boostyApi.feedTag(
                limit = limit,
                offset = offset,
            )

            response.toTags()
        }

    suspend fun getBlogTags(
        blog: String,
    ): Result<Tags> =
        fetchOrError {
            val response: RemoteTagResponse = boostyApi.blogTag(
                blog = blog,
            )

            response.toTags()
        }
}

private fun RemoteTagResponse.toTags(): Tags {
    return Tags(
        data = data.map(),
        extra = extra?.map(),
    )
}

private fun RemoteExtraResponse.map(): Extra? {
    return Extra(
        offset = offset ?: return null,
        isLast = isLast ?: true,
    )
}

private fun RemoteTagResponse.Data?.map(): Tags.Data {
    if (this == null) return Tags.Data(searchTags = emptyList())
    return Tags.Data(
        searchTags = searchTags?.mapNotNull { it.map() }.orEmpty()
    )
}

private fun RemoteTagResponse.SearchTag.map(): Tags.SearchTag? {
    return Tags.SearchTag(
        tag = tag?.map() ?: return null,
        rank = rank ?: 0,
    )
}

private fun RemoteTagResponse.Tag.map(): Tags.Tag? {
    return Tags.Tag(
        id = id ?: return null,
        title = title ?: return null,
    )
}