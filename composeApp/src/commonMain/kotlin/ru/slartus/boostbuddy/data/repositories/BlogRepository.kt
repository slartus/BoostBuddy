package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.data.repositories.models.Offset
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.data.repositories.models.PostData
import ru.slartus.boostbuddy.data.repositories.models.PostResponse
import ru.slartus.boostbuddy.data.repositories.models.PostUser
import ru.slartus.boostbuddy.data.repositories.models.Posts
import ru.slartus.boostbuddy.utils.Response
import ru.slartus.boostbuddy.utils.fetchOrError

internal class BlogRepository(
    private val httpClient: HttpClient,
) {
    suspend fun getData(accessToken: String, url: String, offset: Offset?): Response<Posts> =
        fetchOrError {
            val response: PostResponse =
                httpClient.get("https://api.boosty.to/v1/blog/$url/post/") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    parameter("limit", "25")
                    offset?.let {
                        parameter("offset", "${offset.createdAt}:${offset.postId}")
                    }
                    //  parameter("offset", "0")
                    parameter("comments_limit", "0")
                    parameter("reply_limit", "0")
                }.body()

            Posts(
                items = response.data?.mapNotNull { it.mapToPostOrNull() } ?: emptyList(),
                isLast = response.extra?.isLast == true
            )
        }
}

private fun PostResponse.PostData.mapToPostDataOrNull(): PostData? {
    return PostData(
        vid = vid ?: return null,
        title = title.orEmpty(),
        videoUrls = playerUrls
            ?.map {
                PlayerUrl(it.type ?: return null, it.url ?: return null)
            }
    )
}

private fun PostResponse.Post.mapToPostOrNull(): Post? {
    val videoItems = data
        ?: emptyList()

    val post = Post(
        id = id ?: return null,
        createdAt = createdAt ?: return null,
        intId = intId ?: return null,
        title = title ?: return null,
        data = videoItems.mapNotNull { it.mapToPostDataOrNull() },
        user = user?.let {
            PostUser(
                name = it.name ?: return null,
                avatarUrl = it.avatarUrl
            )
        } ?: return null,
        previewUrl = videoItems.firstOrNull { !it.defaultPreview.isNullOrEmpty() }?.defaultPreview
    )

    return post
}
