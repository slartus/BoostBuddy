package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.call.body
import ru.slartus.boostbuddy.data.repositories.models.BlogInfoResponse
import ru.slartus.boostbuddy.data.repositories.models.Extra
import ru.slartus.boostbuddy.data.repositories.models.Poll
import kotlinx.datetime.Clock
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
    suspend fun fetchPosts(
        url: String,
        limit: Int = 20,
        offset: String? = null,
        commentsLimit: Int = 0,
        replyLimit: Int = 0,
        isOnlyAllowed: Boolean? = false,
        fromDate: Clock? = null,
        toDate: Clock? = null,
        tagsIds: List<Int>? = null,
        onlyBought: Boolean? = null
    ): Result<Posts> =
        fetchOrError {
            val response: PostResponse = boostyApi.blogPosts(
                blog = url,
                limit = 20,
                offset = offset,
                commentsLimit = 0,
                replyLimit = 0
            ).body()
            val response: PostResponse =
                httpClient.get("https://api.boosty.to/v1/blog/$url/post/") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    parameter("limit", limit)
                    parameter("comments_limit", commentsLimit)
                    parameter("reply_limit", replyLimit)
                    if (offset != null)
                        parameter("offset", "${offset.createdAt}:${offset.postId}")
                    if (isOnlyAllowed != null)
                        parameter("is_only_allowed", isOnlyAllowed)
                    if (onlyBought != null)
                        parameter("only_bought", onlyBought)
                    if (fromDate != null)
                        parameter("from_ts", fromDate.now().epochSeconds)
                    if (toDate != null)
                        parameter("to_ts", toDate.now().epochSeconds)
                    if (!tagsIds.isNullOrEmpty()) {
                        parameter("tags_ids", tagsIds.joinToString(","))
                    }
                }.body()

            Posts(
                items = response.data?.mapNotNull { it.mapToPostOrNull() } ?: emptyList(),
                extra = response.extra?.mapToExtraOrNull(),
            )
        }

    suspend fun fetchInfo(blogUrl: String): Result<Blog> =
        fetchOrError {
            val response: BlogInfoResponse = boostyApi.blogInfo(blogUrl).body()

            Blog(
                title = response.title.orEmpty(),
                blogUrl = response.blogUrl ?: error("Blog blogUrl is null"),
                owner = response.owner?.let {
                    Owner(
                        name = it.name.orEmpty(),
                        avatarUrl = it.avatarUrl
                    )
                } ?: error("Blog owner is null"),
            )
        }
}

internal fun PostResponse.Extra.mapToExtraOrNull(): Extra? {
    return Extra(
        offset = offset ?: return null,
        isLast = isLast ?: true
    )
}

internal fun PostResponse.Post.mapToPostOrNull(): Post? {
    return Post(
        id = id ?: return null,
        createdAt = createdAt ?: return null,
        signedQuery = signedQuery.orEmpty(),
        intId = intId ?: return null,
        title = title.orEmpty(),
        data = data?.mapNotNull { it.mapToContentOrNull() }.orEmpty().mergeText(),
        teaser = teaser?.mapNotNull { it.mapToContentOrNull() }.orEmpty().mergeText(),
        user = user?.mapToUserOrNull() ?: return null,
        count = PostCount(likes = count?.likes ?: 0, comments = count?.comments ?: 0),
        poll = poll?.mapToPostPollOrNull(),
        hasAccess = hasAccess ?: true
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