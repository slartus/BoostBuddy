package ru.slartus.boostbuddy.components.blog

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.slartus.boostbuddy.data.repositories.Blog
import ru.slartus.boostbuddy.data.repositories.models.Post

data class BlogViewState(
    val blog: Blog,
    val items: ImmutableList<BlogItem> = persistentListOf(),
    val hasMore: Boolean = true,
    val progressProgressState: ProgressState = ProgressState.Init,
) {
    sealed class ProgressState {
        data object Init : ProgressState()
        data object Loading : ProgressState()
        data object Loaded : ProgressState()
        data class Error(val description: String) : ProgressState()
    }
}

@Immutable
sealed class BlogItem(val key: String, val contentType: String) {
    data class PostItem(val post: Post) : BlogItem(post.id, "post")
    data object LoadingItem : BlogItem("loading", "loading")
    data class ErrorItem(val description: String) : BlogItem("error", "error")
}