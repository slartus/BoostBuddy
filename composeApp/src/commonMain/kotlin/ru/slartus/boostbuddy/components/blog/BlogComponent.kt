package ru.slartus.boostbuddy.components.blog

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.collections.immutable.ImmutableList
import ru.slartus.boostbuddy.components.common.ProgressState
import ru.slartus.boostbuddy.components.feed.FeedPostItem
import ru.slartus.boostbuddy.components.feed.PostsFeedComponent
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.Blog
import ru.slartus.boostbuddy.data.repositories.BlogRepository
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.Offset
import ru.slartus.boostbuddy.data.repositories.models.Poll
import ru.slartus.boostbuddy.data.repositories.models.PollOption
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.data.repositories.models.Posts

@Stable
interface BlogComponent {
    val viewStates: Value<BlogViewState>
    fun onVideoItemClicked(post: Post, postData: Content.OkVideo)
    fun onBackClicked()
    fun onScrolledToEnd()
    fun onRepeatClicked()
    fun onErrorItemClicked()
    fun onCommentsClicked(post: Post)
    fun onPollOptionClicked(post: Post, poll: Poll, pollOption: PollOption)
    fun onVoteClicked(post: Post, poll: Poll)
    fun onDeleteVoteClicked(post: Post, poll: Poll)
}

class BlogComponentImpl(
    componentContext: ComponentContext,
    private val blog: Blog,
    private val onBackClicked: () -> Unit,
) : PostsFeedComponent<BlogViewState, Any>(
    componentContext,
    BlogViewState(blog)
), BlogComponent {
    private val blogRepository by Inject.lazy<BlogRepository>()
    override val viewStateItems: List<FeedPostItem> get() = viewState.items

    override suspend fun fetch(offset: Offset?): Result<Posts> =
        blogRepository.getData(blog.blogUrl, offset)

    override fun onProgressStateChanged(progressState: ProgressState) {
        viewState = viewState.copy(progressState = progressState)
    }

    override fun onNewItems(items: ImmutableList<FeedPostItem>, hasMore: Boolean) {
        viewState = viewState.copy(items = items, hasMore = hasMore)
    }

    override fun onBackClicked() {
        onBackClicked.invoke()
    }

    override fun onScrolledToEnd() {
        fetchNext()
    }

    override fun onRepeatClicked() {
        refresh()
    }
}