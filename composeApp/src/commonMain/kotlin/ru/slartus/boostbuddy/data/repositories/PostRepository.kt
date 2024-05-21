package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.call.body
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.data.repositories.models.PostResponse
import ru.slartus.boostbuddy.utils.fetchOrError

internal class PostRepository(
    private val boostyApi: BoostyApi
) {
    suspend fun getPost(blog: String, id: String): Result<Post> =
        fetchOrError {
            val response: PostResponse.Post = boostyApi.post(
                blog = blog,
                postId = id,
                commentsLimit = 20,
                replyLimit = 20
            ).body()

            response.mapToPostOrNull() ?: error("Ошибка данных")
        }
}