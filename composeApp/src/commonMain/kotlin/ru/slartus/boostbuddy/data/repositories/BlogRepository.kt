package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import ru.slartus.boostbuddy.data.repositories.models.Offset
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.data.repositories.models.PostCount
import ru.slartus.boostbuddy.data.repositories.models.PostResponse
import ru.slartus.boostbuddy.data.repositories.models.Posts
import ru.slartus.boostbuddy.data.repositories.models.User
import ru.slartus.boostbuddy.data.repositories.models.mapToContentOrNull
import ru.slartus.boostbuddy.data.repositories.models.mergeText
import ru.slartus.boostbuddy.utils.fetchOrError

internal class BlogRepository(
    private val httpClient: HttpClient,
) {
    suspend fun getData(accessToken: String, url: String, offset: Offset?): Result<Posts> =
        fetchOrError {
            val response: PostResponse =
                httpClient.get("https://api.boosty.to/v1/blog/$url/post/") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    parameter("limit", "20")
                    offset?.let {
                        parameter("offset", "${offset.createdAt}:${offset.postId}")
                    }
                    parameter("comments_limit", "0")
                    parameter("reply_limit", "0")
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
        intId = intId ?: return null,
        title = title ?: return null,
        data = data?.mapNotNull { it.mapToContentOrNull() }.orEmpty().mergeText(),
        user = user?.let {
            User(
                name = it.name ?: return null,
                avatarUrl = it.avatarUrl
            )
        } ?: return null,
        count = PostCount(likes = count?.likes ?: 0, comments = count?.comments ?: 0)
    )

    return post
}