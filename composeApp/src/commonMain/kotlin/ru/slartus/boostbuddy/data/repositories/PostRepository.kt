package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.call.body
import ru.slartus.boostbuddy.data.repositories.models.Poll
import ru.slartus.boostbuddy.data.repositories.models.PollResponse
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

    suspend fun pollVote(pollId: Int, optionIds: List<Int>): Result<Unit> =
        fetchOrError {
            boostyApi.pollVote(pollId, optionIds)
        }

    suspend fun deletePollVote(pollId: Int): Result<Unit> =
        fetchOrError {
            boostyApi.deletePollVote(pollId)
        }

    suspend fun getPoll(blog: String, pollId: Int): Result<Poll> =
        fetchOrError {
            val pollResponse: PollResponse = boostyApi.poll(blog, pollId).body()
            pollResponse.data?.poll?.mapToPostPollOrNull() ?: error("Ошибка данных")
        }
}