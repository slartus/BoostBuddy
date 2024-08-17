package ru.slartus.boostbuddy.components.subscribes

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.SubscribeItem
import ru.slartus.boostbuddy.data.repositories.SubscribesRepository
import ru.slartus.boostbuddy.navigation.NavigationRouter
import ru.slartus.boostbuddy.navigation.NavigationTree
import ru.slartus.boostbuddy.navigation.navigateTo
import ru.slartus.boostbuddy.utils.Platform
import ru.slartus.boostbuddy.utils.PlatformConfiguration
import ru.slartus.boostbuddy.utils.messageOrThrow
import ru.slartus.boostbuddy.utils.unauthorizedError

@Stable
interface SubscribesComponent {
    val viewStates: Value<SubscribesViewState>
    fun onItemClicked(item: SubscribeItem)
    fun onBackClicked()
    fun onLogoutClicked()
    fun onRepeatClicked()
    fun onSetDarkModeClicked(value: Boolean)
    fun onRefreshClicked()
    fun onFeedbackClicked()
    fun onSettingsClicked()
}

data class SubscribesViewState(
    val progressProgressState: ProgressState,
) {
    sealed class ProgressState {
        data object Init : ProgressState()
        data object Loading : ProgressState()
        data class Loaded(val items: ImmutableList<SubscribeItem>) : ProgressState()
        data class Error(val description: String) : ProgressState()
    }
}

class SubscribesComponentImpl(
    componentContext: ComponentContext,
    private val onBackClicked: () -> Unit,
) : BaseComponent<SubscribesViewState, Any>(
    componentContext,
    SubscribesViewState(SubscribesViewState.ProgressState.Init)
), SubscribesComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val subscribesRepository by Inject.lazy<SubscribesRepository>()
    private val platformConfiguration by Inject.lazy<PlatformConfiguration>()
    private val navigationRouter by Inject.lazy<NavigationRouter>()

    init {
        checkToken()
        subscribeToken()
    }

    private fun checkToken() {
        scope.launch {
            if (settingsRepository.getAccessToken() == null)
                unauthorizedError()
        }
    }

    private fun subscribeToken() {
        scope.launch {
            settingsRepository.tokenFlow.collect { token ->
                if (token != null)
                    fetchSubscribes()
            }
        }
    }

    private fun fetchSubscribes() {
        viewState =
            viewState.copy(progressProgressState = SubscribesViewState.ProgressState.Loading)

        scope.launch {
            val response = subscribesRepository.getSubscribes()

            viewState = if (response.isFailure) {
                viewState.copy(
                    progressProgressState = SubscribesViewState.ProgressState.Error(
                        response.exceptionOrNull()?.messageOrThrow() ?: "Ошибка загрузки"
                    )
                )
            } else {
                viewState.copy(
                    progressProgressState = SubscribesViewState.ProgressState.Loaded(
                        response.getOrDefault(emptyList()).toImmutableList()
                    )
                )
            }

        }
    }

    private fun refresh() {
        scope.launch {
            val token = settingsRepository.getAccessToken() ?: unauthorizedError()
            fetchSubscribes()
        }
    }

    override fun onItemClicked(item: SubscribeItem) {
        navigationRouter.navigateTo(NavigationTree.Blog(item.blog))
    }

    override fun onBackClicked() {
        onBackClicked.invoke()
    }

    override fun onLogoutClicked() {
        navigationRouter.navigateTo(NavigationTree.Logout)
    }

    override fun onRepeatClicked() {
        refresh()
    }

    override fun onSetDarkModeClicked(value: Boolean) {
        scope.launch {
            settingsRepository.setDarkMode(value)
        }
    }

    override fun onRefreshClicked() {
        refresh()
    }

    override fun onFeedbackClicked() {
        runCatching {
            when (platformConfiguration.platform) {
                Platform.Android,
                Platform.iOS -> platformConfiguration.openBrowser(FORUM_URL) {
                    navigationRouter.navigateTo(NavigationTree.Qr(
                        title = "Обсудить на форуме",
                        url = FORUM_URL
                    ))
                }

                Platform.AndroidTV -> navigationRouter.navigateTo(NavigationTree.Qr(
                    title = "Обсудить на форуме",
                    url = FORUM_URL
                ))
            }
        }.onFailure { error ->
            Napier.e("onFeedbackClicked", error)
            navigationRouter.navigateTo(NavigationTree.Qr(
                title = "Обсудить на форуме",
                url = FORUM_URL
            ))
        }
    }

    override fun onSettingsClicked() {
        navigationRouter.navigateTo(NavigationTree.AppSettings)
    }

    companion object {
        const val FORUM_URL = "https://4pda.to/forum/index.php?showtopic=1085976"
    }
}