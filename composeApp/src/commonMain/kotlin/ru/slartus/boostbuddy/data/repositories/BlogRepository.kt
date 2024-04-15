package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.datetime.Clock
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
    private val httpClient: HttpClient,
) {
    suspend fun getData(
        accessToken: String,
        url: String,
        limit: Int = 20,
        offset: Offset? = null,
        commentsLimit: Int = 0,
        replyLimit: Int = 0,
        isOnlyAllowed: Boolean? = false,
        fromDate: Clock? = null,
        toDate: Clock? = null,
        tagsIds: List<Int>? = null,
        onlyBought: Boolean? = null
    ): Result<Posts> =
        fetchOrError {
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
                isLast = response.extra?.isLast == true
            )
        }
}

private fun PostResponse.Post.mapToPostOrNull(): Post? {
    val post = Post(
        id = id ?: return null,
        createdAt = createdAt ?: return null,
        signedQuery = signedQuery.orEmpty(),
        intId = intId ?: return null,
        title = title ?: return null,
        data = data?.mapNotNull { it.mapToContentOrNull() }.orEmpty().mergeText(),
        user = user?.mapToUserOrNull() ?: return null,
        count = PostCount(likes = count?.likes ?: 0, comments = count?.comments ?: 0)
    )

    return post
}