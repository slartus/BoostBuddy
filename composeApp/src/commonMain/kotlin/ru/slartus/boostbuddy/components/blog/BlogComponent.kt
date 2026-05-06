package ru.slartus.boostbuddy.components.blog

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import kotlinx.atomicfu.atomic
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
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
import ru.slartus.boostbuddy.data.repositories.StreamRepository
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.Extra
import ru.slartus.boostbuddy.data.repositories.models.LiveStream
import ru.slartus.boostbuddy.data.repositories.models.Poll
import ru.slartus.boostbuddy.data.repositories.models.PollOption
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.data.repositories.models.Posts
import ru.slartus.boostbuddy.navigation.NavigationRouter
import ru.slartus.boostbuddy.navigation.NavigationTree
import ru.slartus.boostbuddy.navigation.navigateTo
import ru.slartus.boostbuddy.utils.PlatformConfiguration

@Stable
interface BlogComponent {
    val viewStates: Value<BlogViewState>
    val dialogSlot: Value<ChildSlot<*, DialogChild>>
    fun onVideoItemClicked(post: Post, postData: Content.OkVideo)
    fun onLiveStreamClicked()
    fun onLiveStreamLikeClicked()
    fun onLiveStreamShareClicked()
    fun onBackClicked()
    fun onScrolledToEnd()
    fun onRepeatClicked()
    fun onErrorItemClicked()
    fun onPullToRefresh()
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
    private val streamRepository by Inject.lazy<StreamRepository>()
    private val navigationRouter by Inject.lazy<NavigationRouter>()
    private val platformConfiguration by Inject.lazy<PlatformConfiguration>()
    private var liveStreamJob: Job? = null
    private var schedulePollingJob: Job? = null
    private val likeRequestInFlight = atomic(false)
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
        if (token != null) {
            fetchLiveStream()
        } else {
            stopSchedulePolling()
            viewState = viewState.copy(liveStream = null)
        }
    }

    private fun fetchBlogInfo() {
        scope.launch {
            val result = blogRepository.fetchInfo(blog.blogUrl)
            if (result.isSuccess) {
                viewState = viewState.copy(blog = result.getOrThrow())
            }
        }
    }

    private fun fetchLiveStream() {
        val previous = liveStreamJob
        liveStreamJob = scope.launch {
            previous?.cancelAndJoin()
            val result = streamRepository.fetchActive(blog.blogUrl)
            coroutineContext.ensureActive()
            if (result.isSuccess) {
                val newStream = result.getOrNull()
                viewState = viewState.copy(liveStream = newStream)
                updateSchedulePolling(newStream)
            }
        }
    }

    private fun updateSchedulePolling(stream: LiveStream?) {
        if (stream?.status is LiveStream.Status.Scheduled) {
            startSchedulePollingIfNeeded()
        } else {
            stopSchedulePolling()
        }
    }

    private fun startSchedulePollingIfNeeded() {
        if (schedulePollingJob?.isActive == true) return
        schedulePollingJob = scope.launch {
            while (isActive) {
                delay(SCHEDULE_POLL_INTERVAL_MS)
                fetchLiveStream()
            }
        }
    }

    private fun stopSchedulePolling() {
        schedulePollingJob?.cancel()
        schedulePollingJob = null
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

    override fun onIsRefreshingChanged(isRefreshing: Boolean) {
        viewState = viewState.copy(isRefreshing = isRefreshing)
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

    override fun onPullToRefresh() {
        super.onPullToRefresh()
        fetchLiveStream()
    }

    override fun onLiveStreamClicked() {
        val stream = viewState.liveStream ?: return
        val video = stream.video
        if (stream.status is LiveStream.Status.Live &&
            stream.hasAccess &&
            video != null &&
            video.playerUrls.isNotEmpty()
        ) {
            navigationRouter.navigateTo(
                NavigationTree.LiveStream(
                    blogUrl = blog.blogUrl,
                    streamId = stream.id,
                    postData = video,
                )
            )
            return
        }
        platformConfiguration.openBrowser(url = streamShareUrl())
    }

    override fun onLiveStreamLikeClicked() {
        if (!likeRequestInFlight.compareAndSet(expect = false, update = true)) return
        val stream = viewState.liveStream
        if (stream == null) {
            likeRequestInFlight.value = false
            return
        }
        val newLiked = !stream.isLiked
        val newCount = (stream.likesCount + if (newLiked) 1 else -1).coerceAtLeast(0)
        viewState = viewState.copy(
            liveStream = stream.copy(isLiked = newLiked, likesCount = newCount),
        )
        scope.launch {
            try {
                val result = streamRepository.setLiked(blog.blogUrl, newLiked)
                if (result.isFailure) {
                    val current = viewState.liveStream ?: return@launch
                    if (current.id == stream.id) {
                        viewState = viewState.copy(
                            liveStream = current.copy(
                                isLiked = stream.isLiked,
                                likesCount = stream.likesCount,
                            ),
                        )
                    }
                }
            } finally {
                likeRequestInFlight.value = false
            }
        }
    }

    override fun onLiveStreamShareClicked() {
        viewState.liveStream ?: return
        platformConfiguration.shareText(text = streamShareUrl(), onError = null)
    }

    private fun streamShareUrl(): String = "https://boosty.to/${blog.blogUrl}/streams/video_stream"

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

    private companion object {
        private const val SCHEDULE_POLL_INTERVAL_MS = 30_000L
    }
}