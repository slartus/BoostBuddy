package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.call.body
import ru.slartus.boostbuddy.data.repositories.models.Posts
import ru.slartus.boostbuddy.utils.fetchOrError

internal class PostRepository(
    private val boostyApi: BoostyApi
) {
    suspend fun getPost(url: String, id: String): Result<Posts> =
        fetchOrError {
            val response: String = boostyApi.post(
                blog = url,
                postId = id,
                commentsLimit = 20,
                replyLimit = 20
            ).body()

            error(response)
        }
}