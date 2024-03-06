package ru.slartus.boostbuddy.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.Blog
import ru.slartus.boostbuddy.data.repositories.BlogRepository
import ru.slartus.boostbuddy.data.repositories.Post
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.getAccessToken

interface BlogComponent {
    val state: Value<BlogViewState>
    fun onItemClicked(post: Post)
}

data class BlogViewState(
    val blog: Blog,
    val progressProgressState: ProgressState,
) {
    companion object {
        internal fun init(blog: Blog): BlogViewState = BlogViewState(blog, ProgressState.Init)
        internal fun loading(blog: Blog): BlogViewState = BlogViewState(blog, ProgressState.Loading)
        internal fun loaded(blog: Blog, items: List<Post>): BlogViewState =
            BlogViewState(blog, ProgressState.Loaded(items))

        internal fun error(blog: Blog): BlogViewState = BlogViewState(blog, ProgressState.Error)
    }

    sealed class ProgressState {
        data object Init : ProgressState()
        data object Loading : ProgressState()
        data class Loaded(val items: List<Post>) : ProgressState()
        data object Error : ProgressState()
    }
}

class BlogComponentImpl(
    componentContext: ComponentContext,
    private val blog: Blog,
    val onItemSelected: (post: Post) -> Unit
) : BlogComponent, ComponentContext by componentContext {
    private val scope = coroutineScope()
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val blogRepository by Inject.lazy<BlogRepository>()

    private val _state = MutableValue(BlogViewState.init(blog))
    override var state: Value<BlogViewState> = _state

    init {
        fetchBlog()
    }

    private fun fetchBlog() {
        _state.value = BlogViewState.loading(blog)
        scope.launch {
            val token = settingsRepository.getAccessToken().orEmpty()
            runCatching {
                val items = blogRepository.getData(accessToken = token, url = blog.blogUrl)
                _state.value = BlogViewState.loaded(blog, items)
            }.onFailure {
                it.printStackTrace()
                _state.value = BlogViewState.error(blog)
            }
        }
    }

    override fun onItemClicked(post: Post) {
        onItemSelected(post)
    }
}