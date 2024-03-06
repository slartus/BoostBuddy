package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class BlogRepository(
    private val httpClient: HttpClient,
) {
    suspend fun getData(accessToken: String, url: String): List<Post> {
        val response: PostResponse =
            httpClient.get("https://api.boosty.to/v1/blog/$url/post/") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("limit", "25")
                //  parameter("offset", "0")
                parameter("comments_limit", "2")
                parameter("reply_limit", "1")
            }.body()

        return response.data?.mapNotNull { it.mapToPostOrNull() } ?: emptyList()
    }
}

data class Post(
    val id: String,
    val createdAt: Long,
    val intId: Long,
    val title: String,
    val data: List<PostData>
)

@Serializable
data class PostData(
    val title: String,
    val videoUrls: List<PlayerUrl>? = null
)

@Serializable
data class PlayerUrl(
    val type: String,
    val url: String
)

private fun PostResponse.Post.mapToPostOrNull(): Post? {
    val post = Post(
        id = id ?: return null,
        createdAt = createdAt ?: return null,
        intId = intId ?: return null,
        title = title ?: return null,
        data = data
            ?.filter { it.type == "ok_video" }
            ?.mapNotNull { it.mapToPostDataOrNull() } ?: emptyList()
    )
    if (post.data.isEmpty()) return null
    return post
}

private fun PostResponse.PostData.mapToPostDataOrNull(): PostData? {
    return PostData(
        title = title.orEmpty(),
        videoUrls = playerUrls
            ?.map {
                PlayerUrl(it.type ?: return null, it.url ?: return null)
            }
    )
}

@Serializable
private data class PostResponse(
    val data: List<Post>? = null,
    val extra: Extra? = null
) {
    @Serializable
    data class Post(
        val id: String? = null,
        val title: String? = null,
        val createdAt: Long? = null,
        @SerialName("int_id") val intId: Long? = null,
        val data: List<PostData>? = null
    )

    @Serializable
    data class PostData(
        val title: String? = null,
        val type: String? = null,//image, text, link, ok_video
        val url: String? = null,
        val playerUrls: List<PlayerUrl>? = null
    )

    @Serializable
    data class PlayerUrl(
        val type: String? = null,// full_hd, high, medium, lowest, hls, ultra_hd, dash, low, tiny,
        val url: String? = null
    )


    @Serializable
    data class Extra(
        val offset: String? = null,
        val isLast: Boolean? = null
    )
}