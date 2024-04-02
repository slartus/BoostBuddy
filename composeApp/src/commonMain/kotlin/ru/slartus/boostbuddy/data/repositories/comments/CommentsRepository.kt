package ru.slartus.boostbuddy.data.repositories.comments

import io.ktor.client.HttpClient

internal class CommentsRepository(
    private val httpClient: HttpClient,
) {
//    suspend fun getComments(
//        accessToken: String,
//        url: String,
//        postId: String,
//        offsetId: String?
//    ): Result<Comment> =
//        fetchOrError {
//            val response: CommentsResponse =
//                httpClient.get("https://api.boosty.to/v1/blog/$url/post/$postId/comment/") {
//                    header(HttpHeaders.Authorization, "Bearer $accessToken")
//                    parameter("limit", "20")
//                    parameter("reply_limit", "2")
//                    parameter("order", "top")
//                    if (offsetId != null)
//                        parameter("offset", "$offsetId")
//                }.body()
//
//            Comment()
//        }
}