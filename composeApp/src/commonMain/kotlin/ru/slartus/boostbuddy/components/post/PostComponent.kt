package ru.slartus.boostbuddy.components.post

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.components.blog.VideoTypeComponent
import ru.slartus.boostbuddy.components.blog.VideoTypeComponentImpl
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.comments.CommentsRepository
import ru.slartus.boostbuddy.data.repositories.comments.models.Comments
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.utils.messageOrThrow
import ru.slartus.boostbuddy.utils.unauthorizedError

@Stable
interface PostComponent {
    fun onRepeatClicked()
    fun onMoreCommentsClicked()
    fun onMoreRepliesClicked(commentItem: PostViewItem.CommentItem)
    fun onVideoItemClicked(postId: String, postData: Content.OkVideo)

    val viewStates: Value<PostViewState>
    val dialogSlot: Value<ChildSlot<*, VideoTypeComponent>>
    val onBackClicked: () -> Unit
}

class PostComponentImpl(
    componentContext: ComponentContext,
    private val blogUrl: String,
    private val post: Post,
    override val onBackClicked: () -> Unit,
    private val onItemSelected: (postId: String, postData: Content.OkVideo, playerUrl: PlayerUrl) -> Unit,
) : BaseComponent<PostViewState, Any>(
    componentContext,
    PostViewState(post)
), PostComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val commentsRepository by Inject.lazy<CommentsRepository>()
    private val dialogNavigation = SlotNavigation<DialogConfig>()

    override val dialogSlot: Value<ChildSlot<*, VideoTypeComponent>> =
        childSlot(
            source = dialogNavigation,
            serializer = DialogConfig.serializer(), // Or null to disable navigation state saving
            handleBackButton = true, // Close the dialog on back button press
        ) { config, _ ->
            VideoTypeComponentImpl(
                postData = config.postData,
                onDismissed = dialogNavigation::dismiss,
                onItemClicked = { playerUrl ->
                    dialogNavigation.dismiss()
                    onItemSelected(config.postId, config.postData, playerUrl)
                }
            )
        }

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
            if (offsetId == null) viewState.copy(progressProgressState = PostViewState.ProgressState.Loading)
            else viewState.copy(items = buildList {
                add(PostViewItem.LoadingMore)
                addAll(viewState.comments)
            }.toImmutableList())
        scope.launch {
            val response = commentsRepository.getComments(
                url = blogUrl,
                postId = post.id,
                offsetId = offsetId
            )
            if (response.isSuccess) {
                val data = response.getOrNull()
                val newItems = buildList {
                    if (data?.hasMore == true)
                        add(PostViewItem.LoadMore)
                    addAll(data?.comments.orEmpty().map { PostViewItem.CommentItem(it) })
                    if (offsetId != null)
                        addAll(viewState.comments)
                }
                    .distinctBy { it.id }
                    .toImmutableList()
                viewState =
                    viewState.copy(
                        items = newItems,
                        progressProgressState = PostViewState.ProgressState.Loaded
                    )
            } else {
                viewState = if (offsetId == null)
                    viewState.copy(
                        progressProgressState = PostViewState.ProgressState.Error(
                            response.exceptionOrNull()?.messageOrThrow() ?: "Ошибка загрузки"
                        )
                    )
                else
                    viewState.copy(items = buildList {
                        add(PostViewItem.ErrorMore)
                        addAll(viewState.comments)
                    }.toImmutableList())
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

    override fun onMoreRepliesClicked(commentItem: PostViewItem.CommentItem) {
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
                    items = viewState.comments.map { item ->
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

    override fun onVideoItemClicked(postId: String, postData: Content.OkVideo) {
        dialogNavigation.activate(DialogConfig(postId = postId, postData = postData))
    }


    @Serializable
    private data class DialogConfig(
        val postId: String,
        val postData: Content.OkVideo,
    )
}