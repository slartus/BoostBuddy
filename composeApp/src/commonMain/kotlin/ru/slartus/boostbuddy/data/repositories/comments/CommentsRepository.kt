package ru.slartus.boostbuddy.data.repositories.comments

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import ru.slartus.boostbuddy.data.repositories.comments.models.Comment
import ru.slartus.boostbuddy.data.repositories.comments.models.CommentsResponse
import ru.slartus.boostbuddy.data.repositories.comments.models.mapToComments
import ru.slartus.boostbuddy.utils.fetchOrError

internal class CommentsRepository(
    private val httpClient: HttpClient,
) {
    suspend fun getComments(
        accessToken: String,
        url: String,
        postId: String,
        offsetId: String?
    ): Result<List<Comment>> =
        fetchOrError {
            val response: CommentsResponse =
                httpClient.get("https://api.boosty.to/v1/blog/$url/post/$postId/comment/") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    parameter("limit", "20")
                    parameter("reply_limit", "10")
                    parameter("order", "top")
                    if (offsetId != null)
                        parameter("offset", "$offsetId")
                }.body()

            response.mapToComments()
        }
}