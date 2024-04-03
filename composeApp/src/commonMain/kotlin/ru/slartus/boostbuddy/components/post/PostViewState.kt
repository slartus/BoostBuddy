package ru.slartus.boostbuddy.components.post

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import ru.slartus.boostbuddy.data.repositories.comments.models.Comment
import ru.slartus.boostbuddy.data.repositories.models.Post

data class PostViewState(
    val post: Post,
    val progressProgressState: ProgressState = ProgressState.Init,
    val comments: ImmutableList<CommentItem> = persistentListOf(),
    val hasMoreComments: Boolean = true
) {
    sealed class ProgressState {
        data object Init : ProgressState()
        data object Loading : ProgressState()
        data object Loaded : ProgressState()
        data class Error(val description: String) : ProgressState()
    }
}

@Immutable
data class CommentItem(val comment: Comment)