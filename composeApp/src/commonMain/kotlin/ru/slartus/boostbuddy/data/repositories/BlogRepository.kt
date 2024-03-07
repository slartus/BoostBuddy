package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.utils.Response
import ru.slartus.boostbuddy.utils.fetchOrError

class BlogRepository(
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

data class Offset(
    val postId: Long,
    val createdAt: Long
)

data class Posts(
    val items: List<Post>,
    val isLast: Boolean
)

data class Post(
    val id: String,
    val createdAt: Long,
    val intId: Long,
    val title: String,
    val data: List<PostData>,
    val user: PostUser,
    val previewUrl: String?
)

data class PostUser(
    val name: String,
    val avatarUrl: String?
)

@Serializable
data class PostData(
    val vid: String,
    val title: String,
    val videoUrls: List<PlayerUrl>? = null
)

@Serializable
data class PlayerUrl(
    val type: String,
    val url: String
)

private fun PostResponse.Post.mapToPostOrNull(): Post? {
    val videoItems = data
        ?.filter { it.type == "ok_video" } ?: emptyList()

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
    if (post.data.isEmpty()) return null
    return post
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
        val data: List<PostData>? = null,
        val user: PostUser? = null,
        val teaser: List<Teaser>? = null
    )


    @Serializable
    data class Teaser(
        val url: String? = null,
        val type: String? = null,//image
    )

    @Serializable
    data class PostData(
        val vid: String? = null,
        val title: String? = null,
        val type: String? = null,//image, text, link, ok_video
        val url: String? = null,
        val defaultPreview: String? = null,
        val playerUrls: List<PlayerUrl>? = null
    )

    @Serializable
    data class PostUser(
        val name: String? = null,
        val avatarUrl: String? = null
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