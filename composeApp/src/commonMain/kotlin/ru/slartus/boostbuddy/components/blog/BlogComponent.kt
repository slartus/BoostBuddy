package ru.slartus.boostbuddy.components.blog

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.Blog
import ru.slartus.boostbuddy.data.repositories.BlogRepository
import ru.slartus.boostbuddy.data.repositories.PostRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.Poll
import ru.slartus.boostbuddy.data.repositories.models.PollOption
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.utils.messageOrThrow
import ru.slartus.boostbuddy.utils.unauthorizedError

@Stable
interface BlogComponent {
    val viewStates: Value<BlogViewState>
    val dialogSlot: Value<ChildSlot<*, VideoTypeComponent>>
    fun onVideoItemClicked(postId: String, postData: Content.OkVideo)
    fun onBackClicked()
    fun onScrolledToEnd()
    fun onRepeatClicked()
    fun onErrorItemClicked()
    fun onCommentsClicked(post: Post)
    fun onPollOptionClicked(poll: Poll, pollOption: PollOption)
    fun onVoteClicked(poll: Poll)
    fun onDeleteVoteClicked(poll: Poll)
}

class BlogComponentImpl(
    componentContext: ComponentContext,
    private val blog: Blog,
    private val onItemSelected: (postId: String, postData: Content.OkVideo, playerUrl: PlayerUrl) -> Unit,
    private val onBackClicked: () -> Unit,
    private val onPostSelected: (blog: Blog, item: Post) -> Unit,
) : BaseComponent<BlogViewState, Any>(
    componentContext,
    BlogViewState(blog)
), BlogComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val blogRepository by Inject.lazy<BlogRepository>()
    private val postRepository by Inject.lazy<PostRepository>()
    private val dialogNavigation = SlotNavigation<DialogConfig>()

    override val dialogSlot: Value<ChildSlot<*, VideoTypeComponent>> =
        childSlot(
            source = dialogNavigation,
            serializer = DialogConfig.serializer(), // Or null to disable navigation state saving
            handleBackButton = true, // Close the dialog on back button press
        ) { config, _ ->
            VideoTypeComponentImpl(
                componentContext = this,
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
                    fetchBlog()
            }
        }
    }

    private fun fetchBlog() {
        viewState =
            viewState.copy(progressProgressState = BlogViewState.ProgressState.Loading)
        scope.launch {
            val response = blogRepository.getData(
                url = blog.blogUrl,
                offset = null
            )
            if (response.isSuccess) {
                val data = response.getOrNull()
                val newItems = data?.items.orEmpty()
                    .map { BlogItem.PostItem(it) }
                    .toImmutableList()
                viewState =
                    viewState.copy(
                        items = newItems,
                        extra = data?.extra,
                        progressProgressState = BlogViewState.ProgressState.Loaded
                    )
            } else {
                viewState =
                    viewState.copy(
                        progressProgressState = BlogViewState.ProgressState.Error(
                            response.exceptionOrNull()?.messageOrThrow() ?: "Ошибка загрузки"
                        )
                    )
            }
        }
    }

    private fun fetchBlog(offset: String? = null) {
        viewState = viewState.copy(items = viewState.items.plusItem(BlogItem.LoadingItem))
        scope.launch {
            val response = blogRepository.getData(
                url = blog.blogUrl,
                offset = offset
            )
            if (response.isSuccess) {
                val data = response.getOrNull()
                val newItems =
                    viewState.items.plusItems(data?.items.orEmpty().map { BlogItem.PostItem(it) })

                viewState =
                    viewState.copy(
                        items = newItems,
                        extra = data?.extra,
                        progressProgressState = BlogViewState.ProgressState.Loaded
                    )
            } else {
                viewState =
                    viewState.copy(
                        items = viewState.items.plusItem(
                            BlogItem.ErrorItem(
                                response.exceptionOrNull()?.messageOrThrow() ?: "Ошибка загрузки"
                            )
                        )
                    )
            }
        }
    }

    private fun fetchNext() {
        scope.launch {
            settingsRepository.getAccessToken() ?: unauthorizedError()
            fetchBlog(viewState.extra?.offset)
        }
    }

    override fun onVideoItemClicked(postId: String, postData: Content.OkVideo) {
        dialogNavigation.activate(DialogConfig(postId, postData = postData))
    }

    override fun onBackClicked() {
        onBackClicked.invoke()
    }

    override fun onScrolledToEnd() {
        fetchNext()
    }

    override fun onRepeatClicked() {
        scope.launch {
            settingsRepository.getAccessToken() ?: unauthorizedError()
            fetchBlog()
        }
    }

    override fun onErrorItemClicked() {
        fetchNext()
    }

    override fun onCommentsClicked(post: Post) {
        onPostSelected(blog, post)
    }

    private suspend fun refreshPoll(pollId: Int) {
        val pollResponse = postRepository.getPoll(blog.blogUrl, pollId)
        if (pollResponse.isSuccess) {
            val updatedPoll = pollResponse.getOrThrow()
            replacePoll(updatedPoll)
        }
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

                refreshPoll(poll.id)
            }
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
            items = viewState.items.map { item ->
                if (item is BlogItem.PostItem && item.post.poll?.id == newPoll.id)
                    item.copy(post = item.post.copy(poll = newPoll))
                else item
            }.toImmutableList()
        )
    }

    companion object {
        private fun List<BlogItem>.plusItems(items: List<BlogItem>): ImmutableList<BlogItem> =
            dropLastWhile { it !is BlogItem.PostItem }
                .plus(items)
                .distinctBy { it.key }
                .toImmutableList()

        private fun List<BlogItem>.plusItem(item: BlogItem): ImmutableList<BlogItem> =
            dropLastWhile { it !is BlogItem.PostItem }
                .plus(item)
                .distinctBy { it.key }
                .toImmutableList()
    }

    @Serializable
    private data class DialogConfig(
        val postId: String,
        val postData: Content.OkVideo,
    )
}