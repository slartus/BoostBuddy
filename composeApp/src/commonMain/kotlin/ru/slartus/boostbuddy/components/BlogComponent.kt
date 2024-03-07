package ru.slartus.boostbuddy.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.Blog
import ru.slartus.boostbuddy.data.repositories.BlogRepository
import ru.slartus.boostbuddy.data.repositories.Offset
import ru.slartus.boostbuddy.data.repositories.Post
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.utils.Response
import ru.slartus.boostbuddy.utils.messageOrThrow
import ru.slartus.boostbuddy.utils.unauthorizedError

interface BlogComponent {
    val viewStates: Value<BlogViewState>
    fun onItemClicked(post: Post)
    fun onBackClicked()
    fun onScrolledToEnd()
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
    private val onItemSelected: (post: Post) -> Unit,
    private val onBackClicked: () -> Unit,
) : BaseComponent<BlogViewState>(
    componentContext,
    BlogViewState(blog)
), BlogComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val blogRepository by Inject.lazy<BlogRepository>()

    init {
        subscribeToken()
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

    override fun onItemClicked(post: Post) {
        onItemSelected(post)
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
}