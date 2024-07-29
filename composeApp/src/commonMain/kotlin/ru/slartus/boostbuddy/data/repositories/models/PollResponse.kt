package ru.slartus.boostbuddy.data.repositories.models

import kotlinx.serialization.Serializable

@Serializable
internal data class PollResponse(
    val data: Data? = null
) {
    @Serializable
    data class Data(
        val poll: PostResponse.Poll? = null
    )
}