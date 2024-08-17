package ru.slartus.boostbuddy.navigation

import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.Post

sealed interface Screen

object NavigationTree {
    data object Main : Screen
    data object Subscribes : Screen
    class Blog(val blog: ru.slartus.boostbuddy.data.repositories.Blog) : Screen
    class BlogPost(val blogUrl: String, val post: Post) :
        Screen

    class Video(
        val blogUrl: String,
        val postId: String,
        val postData: Content.OkVideo,
        val playerUrl: PlayerUrl
    ) : Screen

    data object AppSettings : Screen

    data object Logout : Screen
    class Qr(val title: String, val url: String) : Screen
    class VideoType(
        val blogUrl: String,
        val postId: String,
        val postData: Content.OkVideo,
    ) : Screen
}
