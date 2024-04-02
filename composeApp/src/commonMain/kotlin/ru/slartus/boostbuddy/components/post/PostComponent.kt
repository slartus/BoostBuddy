package ru.slartus.boostbuddy.components.post

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.comments.CommentsRepository
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.utils.messageOrThrow

interface PostComponent {
    val viewStates: Value<PostViewState>
    val onBackClicked: () -> Unit
}

class PostComponentImpl(
    componentContext: ComponentContext,
    private val blogUrl: String,
    private val post: Post,
    override val onBackClicked: () -> Unit,
) : BaseComponent<PostViewState, Any>(
    componentContext,
    PostViewState(post)
), PostComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val commentsRepository by Inject.lazy<CommentsRepository>()

    init {
        subscribeToken()
    }

    private fun subscribeToken() {
        scope.launch {
            settingsRepository.tokenFlow.collect { token ->
                if (token != null)
                    fetchPost(token)
            }
        }
    }

    private fun fetchPost(token: String) {
        viewState =
            viewState.copy(progressProgressState = PostViewState.ProgressState.Loading)
        scope.launch {
            val response = commentsRepository.getComments(
                accessToken = token,
                url = blogUrl,
                postId = post.id,
                offsetId = null
            )
            if (response.isSuccess) {
                val data = response.getOrNull()
                val newItems = data.orEmpty()
                    .map { CommentItem(it) }
                    .toImmutableList()
                viewState =
                    viewState.copy(
                        comments = newItems,
                        progressProgressState = PostViewState.ProgressState.Loaded
                    )
            } else {
                viewState =
                    viewState.copy(
                        progressProgressState = PostViewState.ProgressState.Error(
                            response.exceptionOrNull()?.messageOrThrow() ?: "Ошибка загрузки"
                        )
                    )
            }
        }
    }

}