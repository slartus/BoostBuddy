package ru.slartus.boostbuddy.components.feed

import androidx.compose.runtime.Immutable
import com.arkivanov.decompose.ComponentContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.components.common.ProgressState
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.Blog
import ru.slartus.boostbuddy.data.repositories.Owner
import ru.slartus.boostbuddy.data.repositories.PostRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.Offset
import ru.slartus.boostbuddy.data.repositories.models.Poll
import ru.slartus.boostbuddy.data.repositories.models.PollOption
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.data.repositories.models.Posts
import ru.slartus.boostbuddy.navigation.NavigationRouter
import ru.slartus.boostbuddy.navigation.NavigationTree
import ru.slartus.boostbuddy.navigation.navigateTo
import ru.slartus.boostbuddy.utils.messageOrThrow
import ru.slartus.boostbuddy.utils.unauthorizedError

@Immutable
sealed class FeedPostItem(val key: String, val contentType: String) {
    data class PostItem(val post: Post) : FeedPostItem(post.id, "post")
    data object LoadingItem : FeedPostItem("loading", "loading")
    data class ErrorItem(val description: String) : FeedPostItem("error", "error")
}

abstract class PostsFeedComponent<State : Any, Action>(
    componentContext: ComponentContext,
    initState: State,
) : BaseComponent<State, Action>(
    componentContext,
    initState
) {
    private val navigationRouter by Inject.lazy<NavigationRouter>()
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val postRepository by Inject.lazy<PostRepository>()

    protected abstract val viewStateItems: List<FeedPostItem>

    protected fun subscribeToken() {
        scope.launch {
            settingsRepository.tokenFlow.collect { token ->
                tokenChanged(token)
            }
        }
    }

    protected open fun tokenChanged(token: String?) {
        if (token != null)
            fetchData()
    }

    protected abstract suspend fun fetch(offset: Offset?): Result<Posts>

    protected abstract fun onProgressStateChanged(progressState: ProgressState)

    protected abstract fun onNewItems(
        items: ImmutableList<FeedPostItem>,
        hasMore: Boolean = true
    )

    private fun fetchData() {
        onProgressStateChanged(ProgressState.Loading)
        scope.launch {
            val response = fetch(null)
            if (response.isSuccess) {
                val data = response.getOrNull()
                val newItems = data?.items.orEmpty()
                    .map { FeedPostItem.PostItem(it) }
                    .toImmutableList()
                onNewItems(items = newItems, hasMore = data?.isLast != true)
                onProgressStateChanged(ProgressState.Loaded)
            } else {
                onProgressStateChanged(
                    ProgressState.Error(
                        response.exceptionOrNull()?.messageOrThrow() ?: "Ошибка загрузки"
                    )
                )
            }
        }
    }

    private fun fetchData(offset: Offset) {
        onNewItems(viewStateItems.plusItem(FeedPostItem.LoadingItem))
        scope.launch {
            val response = fetch(offset = offset)
            if (response.isSuccess) {
                val data = response.getOrNull()
                val newItems =
                    viewStateItems
                        .plusItems(data?.items.orEmpty().map { FeedPostItem.PostItem(it) })

                onNewItems(newItems, data?.isLast != true)
                onProgressStateChanged(ProgressState.Loaded)
            } else {
                onNewItems(
                    viewStateItems.plusItem(
                        FeedPostItem.ErrorItem(
                            response.exceptionOrNull()?.messageOrThrow() ?: "Ошибка загрузки"
                        )
                    ), true
                )
            }
        }
    }

    protected fun fetchNext() {
        scope.launch {
            settingsRepository.getAccessToken() ?: unauthorizedError()
            val lastItem = viewStateItems.filterIsInstance<FeedPostItem.PostItem>().last()
            val offset = Offset(lastItem.post.intId, lastItem.post.createdAt)
            fetchData(offset)
        }
    }

    fun onVideoItemClicked(post: Post, postData: Content.OkVideo) {
        navigationRouter.navigateTo(
            NavigationTree.VideoType(
                blogUrl = post.user.blogUrl,
                postId = post.id,
                postData = postData
            )
        )
    }

    fun refresh() {
        scope.launch {
            settingsRepository.getAccessToken() ?: unauthorizedError()
            fetchData()
        }
    }

    fun onErrorItemClicked() {
        fetchNext()
    }

    fun onCommentsClicked(post: Post) {
        navigationRouter.navigateTo(NavigationTree.BlogPost(post))
    }

    private suspend fun refreshPoll(post: Post, pollId: Int) {
        val pollResponse = postRepository.getPoll(post.user.blogUrl, pollId)
        if (pollResponse.isSuccess) {
            val updatedPoll = pollResponse.getOrThrow()
            replacePoll(updatedPoll)
        }
    }

    fun onPollOptionClicked(post: Post, poll: Poll, pollOption: PollOption) {
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

                refreshPoll(post, poll.id)
            }
        }
    }

    fun onVoteClicked(post: Post, poll: Poll) {
        scope.launch {
            postRepository.pollVote(poll.id, poll.checked.toList())

            refreshPoll(post, poll.id)
        }
    }

    fun onDeleteVoteClicked(post: Post, poll: Poll) {
        scope.launch {
            postRepository.deletePollVote(poll.id)

            refreshPoll(post, poll.id)
        }
    }

    fun onBlogClicked(post: Post) {
        navigationRouter.navigateTo(
            NavigationTree.Blog(
                Blog(
                    title = "",
                    blogUrl = post.user.blogUrl,
                    owner = Owner(
                        name = post.user.name,
                        avatarUrl = post.user.avatarUrl
                    )
                )
            )
        )
    }

    private fun replacePoll(newPoll: Poll) {
        onNewItems(viewStateItems.map { item ->
            if (item is FeedPostItem.PostItem && item.post.poll?.id == newPoll.id)
                item.copy(post = item.post.copy(poll = newPoll))
            else item
        }.toImmutableList())
    }

    companion object {
        private fun List<FeedPostItem>.plusItems(items: List<FeedPostItem>): ImmutableList<FeedPostItem> =
            dropLastWhile { it !is FeedPostItem.PostItem }
                .plus(items)
                .distinctBy { it.key }
                .toImmutableList()

        private fun List<FeedPostItem>.plusItem(item: FeedPostItem): ImmutableList<FeedPostItem> =
            dropLastWhile { it !is FeedPostItem.PostItem }
                .plus(item)
                .distinctBy { it.key }
                .toImmutableList()
    }
}