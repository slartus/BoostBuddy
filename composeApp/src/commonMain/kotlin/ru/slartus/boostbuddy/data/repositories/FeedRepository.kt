package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.call.body
import ru.slartus.boostbuddy.data.repositories.models.FeedResponse
import ru.slartus.boostbuddy.data.repositories.models.Posts
import ru.slartus.boostbuddy.utils.fetchOrError

internal class FeedRepository(
    private val boostyApi: BoostyApi,
) {
    suspend fun getData(offset: String?): Result<Posts> =
        fetchOrError {
            val response: FeedResponse = boostyApi.feed(
                limit = 10,
                offset = offset,
                commentsLimit = 0,
                replyLimit = 0
            ).body()

            Posts(
                items = response.data?.posts?.mapNotNull { it.mapToPostOrNull() } ?: emptyList(),
                extra = response.extra?.mapToExtraOrNull()
            )
        }
}