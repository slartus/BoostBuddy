package ru.slartus.boostbuddy.components.feed

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.collections.immutable.ImmutableList
import ru.slartus.boostbuddy.components.common.ProgressState
import ru.slartus.boostbuddy.components.filter.Filter
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.FeedRepository
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.Extra
import ru.slartus.boostbuddy.data.repositories.models.Poll
import ru.slartus.boostbuddy.data.repositories.models.PollOption
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.data.repositories.models.Posts

@Stable
interface FeedComponent {
    val viewStates: Value<FeedViewState>
    val filter: Filter
    fun refresh()
    fun onVideoItemClicked(post: Post, postData: Content.OkVideo)
    fun onScrolledToEnd()
    fun onRepeatClicked()
    fun onErrorItemClicked()
    fun onCommentsClicked(post: Post)
    fun onPollOptionClicked(post: Post, poll: Poll, pollOption: PollOption)
    fun onVoteClicked(post: Post, poll: Poll)
    fun onDeleteVoteClicked(post: Post, poll: Poll)
    fun onBlogClicked(post: Post)
    fun filter(filter: Filter)
    fun search(query: String)
}

class FeedComponentImpl(
    componentContext: ComponentContext,
) : PostsFeedComponent<FeedViewState, Any>(
    componentContext,
    FeedViewState()
), FeedComponent {
    private val feedRepository by Inject.lazy<FeedRepository>()
    override val extra: Extra?
        get() = viewState.extra
    override val viewStateItems: List<FeedPostItem> get() = viewState.items

    override val filter: Filter
        get() = viewState.filter

    init {
        subscribeToken()
    }

    override suspend fun fetch(offset: String?): Result<Posts> =
        if (viewState.searchQuery.isEmpty()) {
            feedRepository.getData(
                offset = offset,
                filter = viewState.filter,
            )
        } else {
            feedRepository.searchData(
                offset = offset,
                filter = viewState.filter,
                query = viewState.searchQuery
            )
        }


    override fun onProgressStateChanged(progressState: ProgressState) {
        viewState = viewState.copy(progressState = progressState)
    }

    override fun onNewItems(items: ImmutableList<FeedPostItem>, extra: Extra?) {
        viewState = viewState.copy(items = items, extra = extra)
    }

    override fun onScrolledToEnd() {
        fetchNext()
    }

    override fun onRepeatClicked() {
        refresh()
    }

    override fun filter(filter: Filter) {
        viewState = viewState.copy(
            filter = filter,
            extra = null,
        )
        refresh()
    }

    override fun search(query: String) {
        viewState = viewState.copy(
            searchQuery = query,
        )
        refresh()
    }
}