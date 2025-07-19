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
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.components.common.ProgressState
import ru.slartus.boostbuddy.components.feed.FeedPostItem
import ru.slartus.boostbuddy.components.feed.PostsFeedComponent
import ru.slartus.boostbuddy.components.filter.Filter
import ru.slartus.boostbuddy.components.filter.FilterComponent
import ru.slartus.boostbuddy.components.filter.FilterComponentImpl
import ru.slartus.boostbuddy.components.filter.FilterParams
import ru.slartus.boostbuddy.components.filter.FilterScreenEntryPoint
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.Blog
import ru.slartus.boostbuddy.data.repositories.BlogRepository
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.Extra
import ru.slartus.boostbuddy.data.repositories.models.Poll
import ru.slartus.boostbuddy.data.repositories.models.PollOption
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.data.repositories.models.Posts

@Stable
interface BlogComponent {
    val viewStates: Value<BlogViewState>
    val dialogSlot: Value<ChildSlot<*, DialogChild>>
    fun onVideoItemClicked(post: Post, postData: Content.OkVideo)
    fun onBackClicked()
    fun onScrolledToEnd()
    fun onRepeatClicked()
    fun onErrorItemClicked()
    fun onCommentsClicked(post: Post)
    fun onPollOptionClicked(post: Post, poll: Poll, pollOption: PollOption)
    fun onVoteClicked(post: Post, poll: Poll)
    fun onDeleteVoteClicked(post: Post, poll: Poll)
    fun onFilterClick()
    fun onDialogDismissed()
    fun onSearchQueryChange(query: String)

    sealed class DialogChild {
        data class Filter(val component: FilterComponent) : DialogChild()
    }
}

class BlogComponentImpl(
    componentContext: ComponentContext,
    blog: Blog,
    private val onBackClicked: () -> Unit,
) : PostsFeedComponent<BlogViewState, Any>(
    componentContext,
    BlogViewState(blog)
), BlogComponent {
    private val dialogNavigation = SlotNavigation<DialogConfig>()
    private val blogRepository by Inject.lazy<BlogRepository>()
    override val extra: Extra? get() = viewState.extra
    override val viewStateItems: List<FeedPostItem> get() = viewState.items
    private val blog: Blog get() = viewState.blog

    init {
        subscribeToken()
    }

    override val dialogSlot: Value<ChildSlot<*, BlogComponent.DialogChild>> = childSlot(
        key = "dialogSlot",
        source = dialogNavigation,
        serializer = DialogConfig.serializer(),
        handleBackButton = true,
        childFactory = ::createDialogChild
    )

    private fun createDialogChild(
        config: DialogConfig,
        componentContext: ComponentContext
    ): BlogComponent.DialogChild =
        when (config) {
            is DialogConfig.Filter -> BlogComponent.DialogChild.Filter(
                FilterComponentImpl(
                    componentContext = componentContext,
                    params = FilterParams(
                        filter = viewState.filter,
                        onFilter = ::onFilter,
                        entryPoint = FilterScreenEntryPoint.Blog(blog),
                    ),
                )
            )
        }

    private fun onFilter(filter: Filter) {
        viewState = viewState.copy(
            filter = filter,
            extra = null,
        )
        refresh()
    }

    override fun tokenChanged(token: String?) {
        super.tokenChanged(token)
        fetchBlogInfo()
    }

    private fun fetchBlogInfo() {
        scope.launch {
            val result = blogRepository.fetchInfo(blog.blogUrl)
            if (result.isSuccess) {
                viewState = viewState.copy(blog = result.getOrThrow())
            }
        }
    }

    override suspend fun fetch(offset: String?): Result<Posts> =
        if (viewState.searchQuery.isEmpty()) {
            blogRepository.fetchPosts(
                url = blog.blogUrl,
                offset = offset,
                filter = viewState.filter
            )
        } else {
            blogRepository.searchPosts(
                url = blog.blogUrl,
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

    override fun onBackClicked() {
        onBackClicked.invoke()
    }

    override fun onScrolledToEnd() {
        fetchNext()
    }

    override fun onRepeatClicked() {
        refresh()
    }

    override fun onFilterClick() {
        dialogNavigation.activate(DialogConfig.Filter)
    }

    override fun onDialogDismissed() {
        dialogNavigation.dismiss()
    }

    override fun onSearchQueryChange(query: String) {
        viewState = viewState.copy(searchQuery = query)
        refresh()
    }

    @Serializable
    private sealed interface DialogConfig {
        object Filter : DialogConfig
    }
}