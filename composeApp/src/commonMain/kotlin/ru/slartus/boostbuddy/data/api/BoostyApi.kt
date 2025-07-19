package ru.slartus.boostbuddy.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters
import kotlinx.datetime.Clock
import ru.slartus.boostbuddy.data.api.model.RemoteBlogInfoResponse
import ru.slartus.boostbuddy.data.api.model.RemoteBlogTagResponse
import ru.slartus.boostbuddy.data.api.model.RemoteFeedResponse
import ru.slartus.boostbuddy.data.api.model.RemoteFeedTagResponse
import ru.slartus.boostbuddy.data.api.model.RemotePostResponse
import ru.slartus.boostbuddy.data.api.model.RemoteSearchResponse

internal class BoostyApi(
    private val httpClient: HttpClient
) {
    @Suppress("unused")
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

    suspend fun current(): HttpResponse = httpClient.get("v1/user/current")

    suspend fun blogPosts(
        blog: String,
        limit: Int,
        offset: String?,
        commentsLimit: Int,
        replyLimit: Int,
        isOnlyAllowed: Boolean?,
        fromDate: Clock?,
        toDate: Clock?,
        tagsIds: List<String>?,
        onlyBought: Boolean?,
    ): RemotePostResponse = httpClient.get("v1/blog/$blog/post/") {
        parameter("limit", limit)
        parameter("comments_limit", commentsLimit)
        parameter("reply_limit", replyLimit)
        if (offset != null)
            parameter("offset", "$offset")
        if (isOnlyAllowed != null)
            parameter("is_only_allowed", isOnlyAllowed)
        if (onlyBought != null)
            parameter("only_bought", onlyBought)
        if (fromDate != null)
            parameter("from_ts", fromDate.now().epochSeconds)
        if (toDate != null)
            parameter("to_ts", toDate.now().epochSeconds)
        if (!tagsIds.isNullOrEmpty()) {
            parameter("tags_ids", tagsIds.joinToString(","))
        }
    }.body()

    suspend fun blogSearchPosts(
        blog: String,
        limit: Int,
        offset: String?,
        commentsLimit: Int,
        replyLimit: Int,
        isOnlyAllowed: Boolean?,
        fromDate: Clock?,
        toDate: Clock?,
        tagsIds: List<String>?,
        onlyBought: Boolean?,
        query: String,
    ): RemoteSearchResponse = httpClient.get("v1/search/blog/post/") {
        parameter("blog_url", blog)
        parameter("limit", limit)
        parameter("comments_limit", commentsLimit)
        parameter("reply_limit", replyLimit)
        parameter("search_query", query)
        if (offset != null)
            parameter("offset", "$offset")
        if (isOnlyAllowed != null)
            parameter("is_only_allowed", isOnlyAllowed)
        if (onlyBought != null)
            parameter("only_bought", onlyBought)
        if (fromDate != null)
            parameter("from_ts", fromDate.now().epochSeconds)
        if (toDate != null)
            parameter("to_ts", toDate.now().epochSeconds)
        if (!tagsIds.isNullOrEmpty()) {
            parameter("tags_ids", tagsIds.joinToString(","))
        }
    }.body()

    suspend fun blogInfo(
        blogUrl: String
    ): RemoteBlogInfoResponse = httpClient.get("v1/blog/$blogUrl").body()

    suspend fun feed(
        limit: Int,
        offset: String?,
        commentsLimit: Int,
        replyLimit: Int,
        isOnlyAllowed: Boolean?,
        onlyBought: Boolean?,
        fromDate: Clock?,
        toDate: Clock?,
        tagsIds: List<String>,
    ): RemoteFeedResponse = httpClient.get("v1/feed/post/") {
        parameter("limit", limit)
        offset?.let {
            parameter("offset", offset)
        }
        parameter("comments_limit", commentsLimit)
        parameter("reply_limit", replyLimit)
        if (isOnlyAllowed != null)
            parameter("only_allowed", isOnlyAllowed)
        if (onlyBought != null)
            parameter("only_bought", onlyBought)
        if (tagsIds.isNotEmpty())
            parameter("tags_ids", tagsIds.joinToString(separator = ","))
        if (fromDate != null)
            parameter("from_ts", fromDate.now().epochSeconds)
        if (toDate != null)
            parameter("to_ts", toDate.now().epochSeconds)
    }.body()

    suspend fun feedSearch(
        limit: Int,
        offset: String?,
        commentsLimit: Int,
        replyLimit: Int,
        isOnlyAllowed: Boolean?,
        onlyBought: Boolean?,
        fromDate: Clock?,
        toDate: Clock?,
        tagsIds: List<String>,
        query: String,
    ): RemoteSearchResponse = httpClient.get("v1/search/feed/post/") {
        parameter("limit", limit)
        offset?.let {
            parameter("offset", offset)
        }
        parameter("comments_limit", commentsLimit)
        parameter("reply_limit", replyLimit)
        if (isOnlyAllowed != null)
            parameter("only_allowed", isOnlyAllowed)
        if (onlyBought != null)
            parameter("only_bought", onlyBought)
        if (tagsIds.isNotEmpty())
            parameter("tags_ids", tagsIds.joinToString(separator = ","))
        if (fromDate != null)
            parameter("from_ts", fromDate.now().epochSeconds)
        if (toDate != null)
            parameter("to_ts", toDate.now().epochSeconds)
        parameter("search_query", query)
    }.body()

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

    suspend fun pollVote(
        pollId: Int,
        optionIds: List<Int>
    ): HttpResponse = httpClient.post("v1/poll/$pollId/vote") {
        setBody(FormDataContent(Parameters.build {
            append("answer", optionIds.joinToString(separator = ","))
        }))
    }

    suspend fun deletePollVote(
        pollId: Int
    ): HttpResponse = httpClient.delete("v1/poll/$pollId/vote")

    suspend fun poll(
        blog: String,
        pollId: Int
    ): HttpResponse = httpClient.get("v1/blog/$blog/poll/$pollId")

    suspend fun events(
    ): HttpResponse = httpClient.get("v1/notification/standalone/event/")

    suspend fun feedTag(
        limit: Int,
        offset: String?,
    ): RemoteFeedTagResponse = httpClient.get("v1/search/feed/tag/") {
        parameter("limit", "$limit")
        if (offset != null) {
            parameter("offset", "$offset")
        }
    }.body()

    suspend fun blogTag(
        blog: String,
    ): RemoteBlogTagResponse = httpClient.get("v1/blog/$blog/post/tag/").body()
}