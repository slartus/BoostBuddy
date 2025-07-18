package ru.slartus.boostbuddy.data.api.model

import kotlinx.serialization.Serializable

@Serializable
internal data class RemoteBlogInfoResponse(
    val title: String? = null,
    val blogUrl: String? = null,
    val owner: Owner? = null
) {
    @Serializable
    data class Owner(
        val name: String? = null,
        val avatarUrl: String? = null
    )
}