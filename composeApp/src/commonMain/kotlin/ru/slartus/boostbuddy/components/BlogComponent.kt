package ru.slartus.boostbuddy.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.Blog
import ru.slartus.boostbuddy.data.repositories.BlogRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.models.Offset
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.data.repositories.models.PostData
import ru.slartus.boostbuddy.utils.Response
import ru.slartus.boostbuddy.utils.messageOrThrow
import ru.slartus.boostbuddy.utils.unauthorizedError

interface BlogComponent {
    val viewStates: Value<BlogViewState>
    val dialogSlot: Value<ChildSlot<*, VideoTypeComponent>>
    fun onItemClicked(post: Post)
    fun onBackClicked()
    fun onScrolledToEnd()
    fun onRepeatClicked()
}

data class BlogViewState(
    val blog: Blog,
    val items: ImmutableList<Post> = persistentListOf(),
    val hasMore: Boolean = true,
    val progressProgressState: ProgressState = ProgressState.Init,
) {
    sealed class ProgressState {
        data object Init : ProgressState()
        data object Loading : ProgressState()
        data object Loaded : ProgressState()
        data class Error(val description: String) : ProgressState()
    }
}

class BlogComponentImpl(
    componentContext: ComponentContext,
    private val blog: Blog,
    private val onItemSelected: (postData: PostData, playerUrl: PlayerUrl) -> Unit,
    private val onBackClicked: () -> Unit,
) : BaseComponent<BlogViewState>(
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

    private fun fetchBlog(token: String, offset: Offset? = null) {
        viewState =
            viewState.copy(progressProgressState = BlogViewState.ProgressState.Loading)
        scope.launch {
            when (val response = blogRepository.getData(
                accessToken = token,
                url = blog.blogUrl,
                offset = offset
            )) {
                is Response.Error -> viewState =
                    viewState.copy(
                        progressProgressState = BlogViewState.ProgressState.Error(
                            response.exception.messageOrThrow()
                        )
                    )

                is Response.Success -> {
                    val newItems = (viewState.items + response.data.items)
                        .distinctBy { it.intId }.toImmutableList()
                    viewState =
                        viewState.copy(
                            items = newItems,
                            hasMore = !response.data.isLast,
                            progressProgressState = BlogViewState.ProgressState.Loaded
                        )
                }
            }
        }
    }

    @Serializable
    private data class DialogConfig(
        val postData: PostData,
    )

    override fun onItemClicked(post: Post) {
        val postData = post.data.find { (it.videoUrls?.size ?: 0) > 0 } ?: return
        dialogNavigation.activate(DialogConfig(postData = postData))
    }

    override fun onBackClicked() {
        onBackClicked.invoke()
    }

    override fun onScrolledToEnd() {
        scope.launch {
            val token = settingsRepository.getAccessToken() ?: unauthorizedError()
            val offset = viewState.items.last().let { Offset(it.intId, it.createdAt) }
            fetchBlog(token, offset)
        }
    }

    override fun onRepeatClicked() {
        scope.launch {
            val token = settingsRepository.getAccessToken() ?: unauthorizedError()
            fetchBlog(token)
        }
    }
}