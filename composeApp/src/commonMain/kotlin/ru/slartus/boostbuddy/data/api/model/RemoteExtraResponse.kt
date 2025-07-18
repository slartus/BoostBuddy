package ru.slartus.boostbuddy.data.api.model

import kotlinx.serialization.Serializable

@Serializable
class RemoteExtraResponse(
    val offset: String? = null,
    val isLast: Boolean? = null,
)