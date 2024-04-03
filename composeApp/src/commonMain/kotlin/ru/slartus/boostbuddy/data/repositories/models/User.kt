package ru.slartus.boostbuddy.data.repositories.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val name: String,
    val avatarUrl: String?
)