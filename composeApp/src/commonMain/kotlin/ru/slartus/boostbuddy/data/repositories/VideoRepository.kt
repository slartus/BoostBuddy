package ru.slartus.boostbuddy.data.repositories

import io.github.aakira.napier.Napier

internal class VideoRepository(
    private val boostyApi: BoostyApi
) {
    suspend fun putTimeCode(videoId: String, timeCode: Long) = runCatching {
        boostyApi.putVideoTimeCode(videoId, timeCode)
    }.onFailure {
        Napier.e(it.toString())
    }
}