package ru.slartus.boostbuddy.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.SubscribeItem
import ru.slartus.boostbuddy.data.repositories.SubscribesRepository
import ru.slartus.boostbuddy.utils.Response
import ru.slartus.boostbuddy.utils.WebManager
import ru.slartus.boostbuddy.utils.messageOrThrow
import ru.slartus.boostbuddy.utils.unauthorizedError


interface SubscribesComponent {
    val viewStates: Value<SubscribesViewState>
    fun onItemClicked(item: SubscribeItem)
    fun onBackClicked()
    fun onLogoutClicked()
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
    private val onItemSelected: (item: SubscribeItem) -> Unit,
    private val onBackClicked: () -> Unit,
) : BaseComponent<SubscribesViewState>(
    componentContext,
    SubscribesViewState(SubscribesViewState.ProgressState.Init)
), SubscribesComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val subscribesRepository by Inject.lazy<SubscribesRepository>()


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
                    fetchSubscribes(token)
            }
        }
    }

    private fun fetchSubscribes(token: String) {
        viewState =
            viewState.copy(progressProgressState = SubscribesViewState.ProgressState.Loading)
        scope.launch {
            when (val response = subscribesRepository.getSubscribes(token)) {
                is Response.Error -> viewState =
                    viewState.copy(
                        progressProgressState = SubscribesViewState.ProgressState.Error(
                            response.exception.messageOrThrow()
                        )
                    )

                is Response.Success -> viewState =
                    viewState.copy(
                        progressProgressState = SubscribesViewState.ProgressState.Loaded(
                            response.data.toImmutableList()
                        )
                    )
            }
        }
    }

    override fun onItemClicked(item: SubscribeItem) {
        onItemSelected(item)
    }

    override fun onBackClicked() {
        onBackClicked.invoke()
    }

    override fun onLogoutClicked() {
        scope.launch {
            settingsRepository.putAccessToken(null)
            WebManager.clearWebViewCookies()
            unauthorizedError()
        }
    }
}