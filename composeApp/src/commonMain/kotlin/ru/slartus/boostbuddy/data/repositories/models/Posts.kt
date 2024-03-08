package ru.slartus.boostbuddy.data.repositories.models

import kotlinx.serialization.Serializable


data class Offset(
    val postId: Long,
    val createdAt: Long
)

data class Posts(
    val items: List<Post>,
    val isLast: Boolean
)

data class Post(
    val id: String,
    val createdAt: Long,
    val intId: Long,
    val title: String,
    val data: List<PostData>,
    val user: PostUser,
    val previewUrl: String?
)

data class PostUser(
    val name: String,
    val avatarUrl: String?
)

@Serializable
data class PostData(
    val vid: String,
    val title: String,
    val videoUrls: List<PlayerUrl>? = null
)

@Serializable
data class PlayerUrl(
    val type: String,
    val url: String
)