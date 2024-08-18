package ru.slartus.boostbuddy.data.repositories.models

import kotlinx.serialization.Serializable

@Serializable
internal data class UserResponse(
    val id: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null,
    val blogUrl: String? = null,
)

internal fun UserResponse.mapToUserOrNull(): User? {
    return User(
        name = name ?: return null,
        blogUrl = blogUrl ?: return null,
        avatarUrl = avatarUrl,
    )
}