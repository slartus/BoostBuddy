package ru.slartus.boostbuddy.data.repositories

import ru.slartus.boostbuddy.data.api.BoostyApi
import ru.slartus.boostbuddy.data.api.model.RemoteBlogTagResponse
import ru.slartus.boostbuddy.data.api.model.RemoteExtraResponse
import ru.slartus.boostbuddy.data.api.model.RemoteFeedTagResponse
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
            val response = boostyApi.feedTag(
                limit = limit,
                offset = offset,
            )

            response.toTags()
        }

    suspend fun getBlogTags(
        blog: String,
    ): Result<Tags> =
        fetchOrError {
            val response = boostyApi.blogTag(
                blog = blog,
            )

            response.toTags()
        }
}

private fun RemoteBlogTagResponse.toTags(): Tags {
    return Tags(
        data = Tags.Data(data?.mapNotNull { it.map() }.orEmpty()),
        extra = null,
    )
}


private fun RemoteFeedTagResponse.toTags(): Tags {
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

private fun RemoteFeedTagResponse.Data?.map(): Tags.Data {
    if (this == null) return Tags.Data(searchTags = emptyList())
    return Tags.Data(
        searchTags = searchTags?.mapNotNull { it.map() }.orEmpty()
    )
}

private fun RemoteFeedTagResponse.SearchTag.map(): Tags.SearchTag? {
    return Tags.SearchTag(
        tag = tag?.map() ?: return null,
        rank = rank ?: 0,
    )
}

private fun RemoteFeedTagResponse.Tag.map(): Tags.Tag? {
    return Tags.Tag(
        id = id ?: return null,
        title = title ?: return null,
    )
}

private fun RemoteBlogTagResponse.Tag.map(): Tags.SearchTag? {
    return Tags.SearchTag(
        tag = Tags.Tag(
            id = id ?: return null,
            title = title ?: return null,
        ),
        rank = 0,
    )
}