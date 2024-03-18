package ru.slartus.boostbuddy.components.subscribes

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
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.SubscribeItem
import ru.slartus.boostbuddy.data.repositories.SubscribesRepository
import ru.slartus.boostbuddy.utils.WebManager
import ru.slartus.boostbuddy.utils.messageOrThrow
import ru.slartus.boostbuddy.utils.unauthorizedError


interface SubscribesComponent {
    val viewStates: Value<SubscribesViewState>
    val dialogSlot: Value<ChildSlot<*, LogoutDialogComponent>>
    fun onItemClicked(item: SubscribeItem)
    fun onBackClicked()
    fun onLogoutClicked()
    fun onRepeatClicked()
    fun onSetDarkModeClicked(value: Boolean)
    fun onRefreshClicked()
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
) : BaseComponent<SubscribesViewState, Any>(
    componentContext,
    SubscribesViewState(SubscribesViewState.ProgressState.Init)
), SubscribesComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val subscribesRepository by Inject.lazy<SubscribesRepository>()
    private val dialogNavigation = SlotNavigation<DialogConfig>()

    private val _dialogSlot =
        childSlot<DialogConfig, LogoutDialogComponent>(
            source = dialogNavigation,
            serializer = null,
            handleBackButton = true,
            childFactory = { _, _ ->
                LogoutDialogComponentImpl(
                    onDismissed = dialogNavigation::dismiss,
                    onAcceptClicked = ::logout,
                    onCancelClicked = dialogNavigation::dismiss
                )
            }
        )

    override val dialogSlot: Value<ChildSlot<*, LogoutDialogComponent>> = _dialogSlot

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
            val response = subscribesRepository.getSubscribes(token)

            if (response.isFailure) {
                viewState =
                    viewState.copy(
                        progressProgressState = SubscribesViewState.ProgressState.Error(
                            response.exceptionOrNull()?.messageOrThrow() ?: "Ошибка загрузки"
                        )
                    )
            } else {
                viewState =
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
            fetchSubscribes(token)
        }
    }

    override fun onItemClicked(item: SubscribeItem) {
        onItemSelected(item)
    }

    override fun onBackClicked() {
        onBackClicked.invoke()
    }

    override fun onLogoutClicked() {
        dialogNavigation.activate(DialogConfig)
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

    private fun logout() {
        scope.launch {
            settingsRepository.putAccessToken(null)
            WebManager.clearWebViewCookies()
            unauthorizedError()
        }
    }

    @Serializable
    private object DialogConfig
}