package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import ru.slartus.boostbuddy.data.repositories.models.Posts
import ru.slartus.boostbuddy.utils.fetchOrError

internal class PostRepository(
    private val httpClient: HttpClient,
) {
    suspend fun getPost(url: String, id: String): Result<Posts> =
        fetchOrError {
            val response: String =
                httpClient.get("https://api.boosty.to/v1/blog/$url/post/$id") {
                    parameter("comments_limit", "20")
                    parameter("reply_limit", "20")
                }.body()

            error(response)
        }
}