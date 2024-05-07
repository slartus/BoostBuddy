package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters
import ru.slartus.boostbuddy.data.repositories.models.Offset

internal class BoostyApi(
    private val httpClient: HttpClient
) {
    suspend fun refreshToken(deviceId: String, refreshToken: String): HttpResponse =
        httpClient
            .post("oauth/token/") {
                setBody(FormDataContent(Parameters.build {
                    append("device_id", deviceId)
                    append("device_os", "android")
                    append("grant_type", "refresh_token")
                    append("refresh_token", refreshToken)
                }))
            }.body()

    suspend fun subscribes(limit: Int, withFollow: Boolean): HttpResponse =
        httpClient
            .get("v1/user/subscriptions") {
                parameter("limit", limit)
                parameter("with_follow", withFollow)
            }

    suspend fun blogPosts(
        blog: String,
        limit: Int,
        offset: Offset?,
        commentsLimit: Int,
        replyLimit: Int
    ): HttpResponse = httpClient.get("v1/blog/$blog/post/") {
        parameter("limit", limit)
        offset?.let {
            parameter("offset", "${offset.createdAt}:${offset.postId}")
        }
        parameter("comments_limit", commentsLimit)
        parameter("reply_limit", replyLimit)
    }

    suspend fun post(
        blog: String,
        postId: String,
        commentsLimit: Int,
        replyLimit: Int
    ): HttpResponse = httpClient.get("v1/blog/$blog/post/$postId") {
        parameter("comments_limit", commentsLimit)
        parameter("reply_limit", replyLimit)
    }

    suspend fun comments(
        blog: String,
        postId: String,
        offsetId: Int?,
        parentCommentId: Int? = null
    ): HttpResponse = httpClient.get("v1/blog/$blog/post/$postId/comment/") {
        parameter("limit", "20")
        parameter("reply_limit", "2")
        parameter("order", "top")
        if (offsetId != null)
            parameter("offset", offsetId)
        if (parentCommentId != null)
            parameter("parent_id", parentCommentId)
    }

    suspend fun putVideoTimeCode(
        videoId: String,
        timeCode: Long
    ): HttpResponse = httpClient.put("v1/video/$videoId/timecode/") {
        setBody(FormDataContent(Parameters.build {
            append("time_code", timeCode.toString())
        }))
    }
}