package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import ru.slartus.boostbuddy.data.repositories.models.Offset
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.data.repositories.models.PostData
import ru.slartus.boostbuddy.data.repositories.models.PostDataTextContent
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

val playerUrlsComparator = object : Comparator<PlayerUrl> {
    private val sortedQualities =
        listOf("tiny", "lowest", "low", "hls", "medium", "high", "quad_hd", "full_hd")
            .reversed()

    override fun compare(a: PlayerUrl, b: PlayerUrl): Int =
        sortedQualities.indexOf(a.type).compareTo(sortedQualities.indexOf(b.type))

}

private fun PostResponse.PostData.mapToPostDataOrNull(): PostData? {
    return when (type) {
        "ok_video" -> PostData.OkVideo(
            vid = vid ?: return null,
            title = title.orEmpty(),
            playerUrls = playerUrls?.mapNotNull { it.mapToPlayerUrlOrNull() }.orEmpty()
                .sortedWith(playerUrlsComparator)
                .ifEmpty { return null },
            previewUrl = preview ?: defaultPreview ?: return null,
        )

        "video" -> PostData.Video(
            url = url ?: return null,
        )

        "text" -> {
            PostData.Text(
                content = PostDataTextContent.ofRaw(content.orEmpty()) ?: return null,
                modificator = modificator.orEmpty(),
            )
        }

        "image" -> PostData.Image(
            url = url ?: return null,
        )

        "link" -> PostData.Link(
            rawContent = content.orEmpty(),
            url = url ?: return null,
        )

        "audio_file" -> PostData.AudioFile(
            title = title.orEmpty(),
            url = url ?: return null,
        )

        else -> PostData.Unknown
    }
}

private fun PostResponse.PlayerUrl.mapToPlayerUrlOrNull(): PlayerUrl? {
    return PlayerUrl(type ?: return null, url ?: return null)
}

private fun PostResponse.Post.mapToPostOrNull(): Post? {
    val post = Post(
        id = id ?: return null,
        createdAt = createdAt ?: return null,
        intId = intId ?: return null,
        title = title ?: return null,
        data = data?.mapNotNull { it.mapToPostDataOrNull() }.orEmpty(),
        user = user?.let {
            PostUser(
                name = it.name ?: return null,
                avatarUrl = it.avatarUrl
            )
        } ?: return null
    )

    return post
}
