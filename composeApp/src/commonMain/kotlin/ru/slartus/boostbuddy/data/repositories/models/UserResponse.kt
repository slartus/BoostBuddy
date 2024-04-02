package ru.slartus.boostbuddy.data.repositories.models

import kotlinx.serialization.Serializable

@Serializable
internal data class UserResponse(
    val id: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null
)