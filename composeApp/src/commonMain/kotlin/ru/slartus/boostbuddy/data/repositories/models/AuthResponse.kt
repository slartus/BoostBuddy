package ru.slartus.boostbuddy.data.repositories.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys
data class AuthResponse(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresAt: Long? = null
)