package ru.slartus.boostbuddy.data.repositories

import ru.slartus.boostbuddy.components.filter.AccessType
import ru.slartus.boostbuddy.components.filter.Filter
import ru.slartus.boostbuddy.data.api.BoostyApi
import ru.slartus.boostbuddy.data.repositories.models.Posts
import ru.slartus.boostbuddy.utils.fetchOrError

internal class FeedRepository(
    private val boostyApi: BoostyApi,
) {
    suspend fun getData(
        offset: String?,
        filter: Filter,
    ): Result<Posts> =
        fetchOrError {
            val response = boostyApi.feed(
                limit = 10,
                offset = offset,
                commentsLimit = 0,
                replyLimit = 0,
                onlyBought = filter.accessType == AccessType.Bought,
                isOnlyAllowed = filter.accessType == AccessType.Allowed,
                tagsIds = filter.tags.map { it.id },
                fromDate = filter.period?.from,
                toDate = filter.period?.to,
            )

            Posts(
                items = response.data?.posts?.mapNotNull { it.mapToPostOrNull() } ?: emptyList(),
                extra = response.extra?.mapToExtraOrNull()
            )
        }

    suspend fun searchData(
        offset: String?,
        filter: Filter,
        query: String,
    ): Result<Posts> =
        fetchOrError {
            val response = boostyApi.feedSearch(
                limit = 10,
                offset = offset,
                commentsLimit = 0,
                replyLimit = 0,
                onlyBought = filter.accessType == AccessType.Bought,
                isOnlyAllowed = filter.accessType == AccessType.Allowed,
                tagsIds = filter.tags.map { it.id },
                fromDate = filter.period?.from,
                toDate = filter.period?.to,
                query = query,
            )

            Posts(
                items = response.data?.searchPosts?.mapNotNull { it.post?.mapToPostOrNull() } ?: emptyList(),
                extra = response.extra?.mapToExtraOrNull()
            )
        }
}