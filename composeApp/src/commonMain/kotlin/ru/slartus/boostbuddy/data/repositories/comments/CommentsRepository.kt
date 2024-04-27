package ru.slartus.boostbuddy.data.repositories.comments

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import ru.slartus.boostbuddy.data.repositories.comments.models.Comments
import ru.slartus.boostbuddy.data.repositories.comments.models.CommentsResponse
import ru.slartus.boostbuddy.data.repositories.comments.models.mapToComments
import ru.slartus.boostbuddy.utils.fetchOrError

internal class CommentsRepository(
    private val httpClient: HttpClient,
) {
    suspend fun getComments(
        url: String,
        postId: String,
        offsetId: Int?,
        parentCommentId: Int? = null
    ): Result<Comments> =
        fetchOrError {
            val response: CommentsResponse =
                httpClient.get("https://api.boosty.to/v1/blog/$url/post/$postId/comment/") {
                    parameter("limit", "20")
                    parameter("reply_limit", "2")
                    parameter("order", "top")
                    if (offsetId != null)
                        parameter("offset", offsetId)
                    if (parentCommentId != null)
                        parameter("parent_id", parentCommentId)
                }.body()

            response.mapToComments()
        }
}