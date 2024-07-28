package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.call.body
import ru.slartus.boostbuddy.data.repositories.models.Offset
import ru.slartus.boostbuddy.data.repositories.models.Poll
import ru.slartus.boostbuddy.data.repositories.models.PollOption
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
        title = title.orEmpty(),
        data = data?.mapNotNull { it.mapToContentOrNull() }.orEmpty().mergeText(),
        user = user?.mapToUserOrNull() ?: return null,
        count = PostCount(likes = count?.likes ?: 0, comments = count?.comments ?: 0),
        poll = poll?.mapToPostPollOrNull()
    )
}

internal fun PostResponse.Poll.mapToPostPollOrNull(): Poll? {
    return Poll(
        id = id ?: return null,
        title = title.orEmpty(),
        isMultiple = isMultiple ?: return null,
        isFinished = isFinished ?: return null,
        options = options?.mapNotNull { it.mapToPostPollOptionOrNull() }?.ifEmpty { null }
            ?: return null,
        counter = counter ?: 0,
        answer = answer.orEmpty()
    )
}

private fun PostResponse.PollOption.mapToPostPollOptionOrNull(): PollOption? {
    return PollOption(
        id = id ?: return null,
        text = text.orEmpty(),
        counter = counter ?: return null,
        fraction = fraction ?: return null
    )
}