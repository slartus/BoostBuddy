package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.call.body
import ru.slartus.boostbuddy.data.repositories.models.Offset
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.data.repositories.models.PostCount
import ru.slartus.boostbuddy.data.repositories.models.PostResponse
import ru.slartus.boostbuddy.data.repositories.models.Posts
import ru.slartus.boostbuddy.data.repositories.models.mapToContentOrNull
import ru.slartus.boostbuddy.data.repositories.models.mapToUserOrNull
import ru.slartus.boostbuddy.data.repositories.models.mergeText
import ru.slartus.boostbuddy.utils.fetchOrError

internal class BlogRepository(
    private val boostyApi: BoostyApi,
) {
    suspend fun getData(url: String, offset: Offset?): Result<Posts> =
        fetchOrError {
            val response: PostResponse = boostyApi.blogPosts(
                blog = url,
                limit = 20,
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

internal fun PostResponse.Post.mapToPostOrNull(): Post? {
    return Post(
        id = id ?: return null,
        createdAt = createdAt ?: return null,
        signedQuery = signedQuery.orEmpty(),
        intId = intId ?: return null,
        title = title ?: return null,
        data = data?.mapNotNull { it.mapToContentOrNull() }.orEmpty().mergeText(),
        user = user?.mapToUserOrNull() ?: return null,
        count = PostCount(likes = count?.likes ?: 0, comments = count?.comments ?: 0)
    )
}