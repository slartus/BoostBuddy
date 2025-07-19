package ru.slartus.boostbuddy.data.repositories.models

import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.data.api.model.RemotePostResponse

@Serializable
internal data class PollResponse(
    val data: Data? = null
) {
    @Serializable
    data class Data(
        val poll: RemotePostResponse.Poll? = null
    )
}