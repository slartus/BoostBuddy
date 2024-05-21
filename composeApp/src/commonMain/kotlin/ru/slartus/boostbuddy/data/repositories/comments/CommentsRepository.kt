package ru.slartus.boostbuddy.data.repositories.comments

import io.ktor.client.call.body
import ru.slartus.boostbuddy.data.repositories.BoostyApi
import ru.slartus.boostbuddy.data.repositories.comments.models.Comments
import ru.slartus.boostbuddy.data.repositories.comments.models.CommentsResponse
import ru.slartus.boostbuddy.data.repositories.comments.models.mapToComments
import ru.slartus.boostbuddy.utils.fetchOrError

internal class CommentsRepository(
    private val boostyApi: BoostyApi,
) {
    suspend fun getComments(
        url: String,
        postId: String,
        offsetId: Int?,
        parentCommentId: Int? = null
    ): Result<Comments> =
        fetchOrError {
            val response: CommentsResponse =
                boostyApi.comments(url, postId, offsetId, parentCommentId).body()

            response.mapToComments()
        }
}