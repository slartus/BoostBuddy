package ru.slartus.boostbuddy.components.blog

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
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.models.Offset
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.PostData
import ru.slartus.boostbuddy.utils.messageOrThrow
import ru.slartus.boostbuddy.utils.unauthorizedError

interface BlogComponent {
    val viewStates: Value<BlogViewState>
    val dialogSlot: Value<ChildSlot<*, VideoTypeComponent>>
    fun onVideoItemClicked(postData: PostData.OkVideo)
    fun onBackClicked()
    fun onScrolledToEnd()
    fun onRepeatClicked()
    fun onErrorItemClicked()
}

class BlogComponentImpl(
    componentContext: ComponentContext,
    private val blog: Blog,
    private val onItemSelected: (postData: PostData.OkVideo, playerUrl: PlayerUrl) -> Unit,
    private val onBackClicked: () -> Unit,
) : BaseComponent<BlogViewState, Any>(
    componentContext,
    BlogViewState(blog)
), BlogComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val blogRepository by Inject.lazy<BlogRepository>()
    private val dialogNavigation = SlotNavigation<DialogConfig>()

    init {
        subscribeToken()
    }

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
                    onItemSelected(config.postData, playerUrl)
                }
            )
        }

    private fun subscribeToken() {
        scope.launch {
            settingsRepository.tokenFlow.collect { token ->
                if (token != null)
                    fetchBlog(token)
            }
        }
    }

    private fun fetchBlog(token: String) {
        viewState =
            viewState.copy(progressProgressState = BlogViewState.ProgressState.Loading)
        scope.launch {
            val response = blogRepository.getData(
                accessToken = token,
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
                        hasMore = data?.isLast != true,
                        progressProgressState = BlogViewState.ProgressState.Loaded
                    )
            } else {
                viewState =
                    viewState.copy(
                        progressProgressState = BlogViewState.ProgressState.Error(
                            response.exceptionOrNull()?.messageOrThrow()?: "Ошибка загрузки"
                        )
                    )
            }
        }
    }

    private fun fetchBlog(token: String, offset: Offset? = null) {
        viewState = viewState.copy(items = viewState.items.plusItem(BlogItem.LoadingItem))
        scope.launch {
            val response = blogRepository.getData(
                accessToken = token,
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
                        hasMore = data?.isLast != true,
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
            val token = settingsRepository.getAccessToken() ?: unauthorizedError()
            val lastItem = viewState.items.filterIsInstance<BlogItem.PostItem>().last()
            val offset = Offset(lastItem.post.intId, lastItem.post.createdAt)
            fetchBlog(token, offset)
        }
    }

    override fun onVideoItemClicked(postData: PostData.OkVideo) {
        dialogNavigation.activate(DialogConfig(postData = postData))
    }

    override fun onBackClicked() {
        onBackClicked.invoke()
    }

    override fun onScrolledToEnd() {
        fetchNext()
    }

    override fun onRepeatClicked() {
        scope.launch {
            val token = settingsRepository.getAccessToken() ?: unauthorizedError()
            fetchBlog(token)
        }
    }

    override fun onErrorItemClicked() {
        fetchNext()
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
        val postData: PostData.OkVideo,
    )
}