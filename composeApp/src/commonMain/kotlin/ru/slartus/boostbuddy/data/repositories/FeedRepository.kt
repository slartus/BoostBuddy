package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.call.body
import ru.slartus.boostbuddy.data.repositories.models.Offset
import ru.slartus.boostbuddy.data.repositories.models.PostResponse
import ru.slartus.boostbuddy.data.repositories.models.Posts
import ru.slartus.boostbuddy.utils.fetchOrError

internal class FeedRepository(
    private val boostyApi: BoostyApi,
) {
    suspend fun getData(offset: Offset?): Result<Posts> =
        fetchOrError {
            val response: PostResponse = boostyApi.feed(
                limit = 10,
                offset = offset,
                commentsLimit = 0,
                replyLimit = 0
            ).body()

            Posts(
                items = response.data?.mapNotNull { it.mapToPostOrNull() } ?: emptyList(),
                isLast = response.extra?.isLast == true
            )
        }
}