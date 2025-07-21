package ru.slartus.boostbuddy.data.repositories

import ru.slartus.boostbuddy.data.api.BoostyApi
import ru.slartus.boostbuddy.data.log.logger

internal class VideoRepository(
    private val boostyApi: BoostyApi
) {
    suspend fun putTimeCode(videoId: String, timeCode: Long) = runCatching {
        boostyApi.putVideoTimeCode(videoId, timeCode)
    }.onFailure {
        logger.e(it.toString())
    }
}