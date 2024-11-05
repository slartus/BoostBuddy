package ru.slartus.boostbuddy.components.subscribes

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
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
import ru.slartus.boostbuddy.utils.messageOrThrow
import ru.slartus.boostbuddy.utils.unauthorizedError

@Stable
interface SubscribesComponent {
    val viewStates: Value<SubscribesViewState>
    fun onItemClicked(item: SubscribeItem)
    fun onRepeatClicked()
    fun refresh()
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
    componentContext: ComponentContext
) : BaseComponent<SubscribesViewState, Any>(
    componentContext,
    SubscribesViewState(SubscribesViewState.ProgressState.Init)
), SubscribesComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val subscribesRepository by Inject.lazy<SubscribesRepository>()
    private val navigationRouter by Inject.lazy<NavigationRouter>()

    init {
        subscribeToken()
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

    override fun refresh() {
        scope.launch {
            val token = settingsRepository.getAccessToken() ?: unauthorizedError()
            fetchSubscribes()
        }
    }

    override fun onItemClicked(item: SubscribeItem) {
        navigationRouter.navigateTo(NavigationTree.Blog(item.blog))
    }

    override fun onRepeatClicked() {
        refresh()
    }
}