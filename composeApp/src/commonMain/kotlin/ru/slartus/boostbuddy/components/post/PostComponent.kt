package ru.slartus.boostbuddy.components.post

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.PostRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.comments.CommentsRepository
import ru.slartus.boostbuddy.data.repositories.comments.models.Comments
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.Poll
import ru.slartus.boostbuddy.data.repositories.models.PollOption
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.navigation.NavigationRouter
import ru.slartus.boostbuddy.navigation.NavigationTree
import ru.slartus.boostbuddy.navigation.navigateTo
import ru.slartus.boostbuddy.utils.messageOrThrow
import ru.slartus.boostbuddy.utils.unauthorizedError

@Stable
interface PostComponent {
    fun onRepeatClicked()
    fun onMoreCommentsClicked()
    fun onMoreRepliesClicked(commentItem: PostViewItem.CommentItem)
    fun onVideoItemClicked(postId: String, postData: Content.OkVideo)
    fun onPollOptionClicked(poll: Poll, pollOption: PollOption)
    fun onVoteClicked(poll: Poll)
    fun onDeleteVoteClicked(poll: Poll)

    val viewStates: Value<PostViewState>
    val onBackClicked: () -> Unit
}

class PostComponentImpl(
    componentContext: ComponentContext,
    private val post: Post,
    override val onBackClicked: () -> Unit,
) : BaseComponent<PostViewState, Any>(
    componentContext,
    PostViewState(post)
), PostComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val commentsRepository by Inject.lazy<CommentsRepository>()
    private val postRepository by Inject.lazy<PostRepository>()
    private val navigationRouter by Inject.lazy<NavigationRouter>()
    private val blogUrl: String = post.user.blogUrl

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
        navigationRouter.navigateTo(
            NavigationTree.VideoType(
                blogUrl = blogUrl,
                postId = postId,
                postData = postData
            )
        )
    }

    override fun onPollOptionClicked(poll: Poll, pollOption: PollOption) {
        scope.launch {
            if (poll.isMultiple) {
                val newPoll = if (pollOption.id in poll.checked)
                    poll.copy(checked = poll.checked - pollOption.id)
                else
                    poll.copy(checked = poll.checked + pollOption.id)
                replacePoll(newPoll)
            } else {
                if (pollOption.id in poll.answer)
                    postRepository.deletePollVote(poll.id)
                else
                    postRepository.pollVote(poll.id, listOf(pollOption.id))

                val pollResponse = postRepository.getPoll(blogUrl, poll.id)
                if (pollResponse.isSuccess) {
                    val updatedPoll = pollResponse.getOrThrow()
                    replacePoll(updatedPoll)
                }
            }
        }
    }

    private suspend fun refreshPoll(pollId: Int) {
        val pollResponse = postRepository.getPoll(blogUrl, pollId)
        if (pollResponse.isSuccess) {
            val updatedPoll = pollResponse.getOrThrow()
            replacePoll(updatedPoll)
        }
    }

    override fun onVoteClicked(poll: Poll) {
        scope.launch {
            postRepository.pollVote(poll.id, poll.checked.toList())

            refreshPoll(poll.id)
        }
    }

    override fun onDeleteVoteClicked(poll: Poll) {
        scope.launch {
            postRepository.deletePollVote(poll.id)

            refreshPoll(poll.id)
        }
    }

    private fun replacePoll(newPoll: Poll) {
        viewState = viewState.copy(
            post = viewState.post.copy(poll = newPoll)
        )
    }
}