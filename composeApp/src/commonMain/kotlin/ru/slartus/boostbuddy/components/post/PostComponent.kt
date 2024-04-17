package ru.slartus.boostbuddy.components.post

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.comments.CommentsRepository
import ru.slartus.boostbuddy.data.repositories.comments.models.Comments
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.utils.messageOrThrow
import ru.slartus.boostbuddy.utils.unauthorizedError

interface PostComponent {
    fun onRepeatClicked()
    fun onMoreCommentsClicked()
    fun onMoreRepliesClicked(commentItem: CommentItem)

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
                    fetchPost()
            }
        }
    }

    private fun fetchPost(offsetId: Int? = null) {
        viewState =
            viewState.copy(progressProgressState = PostViewState.ProgressState.Loading)
        scope.launch {
            val response = commentsRepository.getComments(
                url = blogUrl,
                postId = post.id,
                offsetId = offsetId
            )
            if (response.isSuccess) {
                val data = response.getOrNull()
                val newItems = buildList {
                    addAll(data?.comments.orEmpty().map { CommentItem(it) })
                    if (offsetId != null)
                        addAll(viewState.comments)
                }
                    .distinctBy { it.comment.id }
                    .toImmutableList()
                viewState =
                    viewState.copy(
                        comments = newItems,
                        progressProgressState = PostViewState.ProgressState.Loaded,
                        hasMoreComments = data?.hasMore == true
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

    override fun onRepeatClicked() {
        scope.launch {
            settingsRepository.getAccessToken() ?: unauthorizedError()
            fetchPost()
        }
    }

    override fun onMoreCommentsClicked() {
        scope.launch {
            settingsRepository.getAccessToken() ?: unauthorizedError()
            fetchPost(offsetId = viewState.comments.firstOrNull()?.comment?.intId)
        }
    }

    override fun onMoreRepliesClicked(commentItem: CommentItem) {
        scope.launch {
            settingsRepository.getAccessToken() ?: unauthorizedError()
            val response = commentsRepository.getComments(
                url = blogUrl,
                postId = post.id,
                offsetId = commentItem.comment.replies.comments.firstOrNull()?.intId,
                parentCommentId = commentItem.comment.intId
            )

            val data = response.getOrNull() ?: return@launch
            val newItems = data.comments
            viewState =
                viewState.copy(
                    comments = viewState.comments.map { item ->
                        if (item.comment.id == commentItem.comment.id) {
                            item.copy(
                                comment = item.comment.copy(
                                    replies = Comments(
                                        comments = newItems + item.comment.replies.comments,
                                        hasMore = data.hasMore
                                    )
                                )
                            )
                        } else {
                            item
                        }
                    }.toImmutableList()
                )
        }
    }

}