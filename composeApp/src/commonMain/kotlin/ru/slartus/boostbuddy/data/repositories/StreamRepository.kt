package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.call.body
import io.ktor.client.statement.discardRemaining
import io.ktor.http.HttpStatusCode
import ru.slartus.boostbuddy.data.api.BoostyApi
import ru.slartus.boostbuddy.data.api.model.RemoteVideoStreamResponse
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.LiveStream
import ru.slartus.boostbuddy.data.repositories.models.mapToContentOrNull
import ru.slartus.boostbuddy.utils.fetchOrError

internal class StreamRepository(
    private val boostyApi: BoostyApi,
) {
    suspend fun fetchActive(blogUrl: String): Result<LiveStream?> =
        fetchOrError {
            val response = boostyApi.videoStream(blogUrl)
            if (response.status == HttpStatusCode.NoContent) return@fetchOrError null
            val body: RemoteVideoStreamResponse = response.body()
            body.toLiveStream(blogUrl)
        }

    suspend fun heartbeat(blogUrl: String, stop: Boolean): Result<Unit> =
        fetchOrError {
            boostyApi.videoStreamHeartbeat(blogUrl, stop).discardRemaining()
        }

    suspend fun setLiked(blogUrl: String, liked: Boolean): Result<Unit> =
        fetchOrError {
            val response = if (liked) {
                boostyApi.videoStreamLike(blogUrl)
            } else {
                boostyApi.videoStreamUnlike(blogUrl)
            }
            response.discardRemaining()
        }
}

private fun RemoteVideoStreamResponse.toLiveStream(blogUrl: String): LiveStream? {
    val status: LiveStream.Status = when {
        isOnline == true -> LiveStream.Status.Live(startedAt = startTime)
        isOnline == false && isTemplate == true -> LiveStream.Status.Scheduled
        else -> return null
    }
    val id = id ?: return null
    val intId = intId ?: return null
    val coverImageUrl = teaser?.firstNotNullOfOrNull { content ->
        content.takeIf { it.type == "image" }?.url?.takeIf { it.isNotBlank() }
    }
    val video = data?.firstNotNullOfOrNull { content ->
        if (content.type != "ok_stream" && content.type != "ok_video") return@firstNotNullOfOrNull null
        content.mapToContentOrNull() as? Content.OkVideo
    }?.let { okVideo ->
        if (okVideo.previewUrl.isBlank() && coverImageUrl != null) {
            okVideo.copy(previewUrl = coverImageUrl)
        } else {
            okVideo
        }
    }
    return LiveStream(
        id = id,
        intId = intId,
        blogUrl = blogUrl,
        title = title.orEmpty(),
        status = status,
        viewersCount = count?.viewers ?: 0,
        likesCount = count?.likes ?: 0,
        isLiked = isLiked ?: false,
        hasAccess = hasAccess ?: false,
        signedQuery = signedQuery.orEmpty(),
        coverImageUrl = coverImageUrl,
        subscription = subscriptionLevel?.toSubscription(),
        video = video,
    )
}

private fun RemoteVideoStreamResponse.SubscriptionLevel.toSubscription(): LiveStream.Subscription? {
    val name = name ?: return null
    return LiveStream.Subscription(
        name = name,
        priceRub = currencyPrices?.rub,
    )
}
