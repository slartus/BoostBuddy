package ru.slartus.boostbuddy.components.post

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.slartus.boostbuddy.data.repositories.comments.models.Comment
import ru.slartus.boostbuddy.data.repositories.models.Post

@Immutable
data class PostViewState(
    val post: Post,
    val progressProgressState: ProgressState = ProgressState.Init,
    val items: ImmutableList<PostViewItem> = persistentListOf()
) {
    val comments: List<PostViewItem.CommentItem> get() = items.filterIsInstance<PostViewItem.CommentItem>()

    sealed class ProgressState {
        data object Init : ProgressState()
        data object Loading : ProgressState()
        data object Loaded : ProgressState()
        data class Error(val description: String) : ProgressState()
    }
}

@Immutable
sealed class PostViewItem(val id: String) {
    data object LoadMore : PostViewItem("LoadMore")
    data object LoadingMore : PostViewItem("LoadingMore")
    data object ErrorMore : PostViewItem("ErrorMore")
    data class CommentItem(val comment: Comment) : PostViewItem(comment.id)
}