package ru.slartus.boostbuddy.components.subscribes

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.SubscribeItem
import ru.slartus.boostbuddy.data.repositories.SubscribesRepository
import ru.slartus.boostbuddy.utils.Platform
import ru.slartus.boostbuddy.utils.PlatformConfiguration
import ru.slartus.boostbuddy.utils.WebManager
import ru.slartus.boostbuddy.utils.messageOrThrow
import ru.slartus.boostbuddy.utils.unauthorizedError

@Stable
interface SubscribesComponent {
    val viewStates: Value<SubscribesViewState>
    val dialogSlot: Value<ChildSlot<*, DialogChild>>
    fun onItemClicked(item: SubscribeItem)
    fun onBackClicked()
    fun onLogoutClicked()
    fun onRepeatClicked()
    fun onSetDarkModeClicked(value: Boolean)
    fun onRefreshClicked()
    fun onFeedbackClicked()
    fun onDialogDismissed()

    sealed class DialogChild {
        data class Logout(val component: LogoutDialogComponent) : DialogChild()
        data class Qr(val url: String) : DialogChild()
    }
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
    private val platformConfiguration by Inject.lazy<PlatformConfiguration>()
    private val dialogNavigation = SlotNavigation<DialogConfig>()

    private val _dialogSlot = childSlot(
        key = "dialogSlot",
        source = dialogNavigation,
        serializer = DialogConfig.serializer(),
        handleBackButton = true,
        childFactory = ::dialogChild
    )

    override val dialogSlot: Value<ChildSlot<*, SubscribesComponent.DialogChild>> = _dialogSlot

    init {
        checkToken()
        subscribeToken()
    }

    private fun dialogChild(
        config: DialogConfig,
        componentContext: ComponentContext
    ): SubscribesComponent.DialogChild =
        when (config) {
            DialogConfig.Logout -> SubscribesComponent.DialogChild.Logout(
                LogoutDialogComponentImpl(
                    onDismissed = dialogNavigation::dismiss,
                    onAcceptClicked = ::logout,
                    onCancelClicked = dialogNavigation::dismiss
                )
            )

            DialogConfig.Qr -> SubscribesComponent.DialogChild.Qr(FORUM_URL)
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
        dialogNavigation.activate(DialogConfig.Logout)
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
                    dialogNavigation.activate(DialogConfig.Qr)
                }

                Platform.AndroidTV -> dialogNavigation.activate(DialogConfig.Qr)
            }
        }.onFailure { error ->
            Napier.e("onFeedbackClicked", error)
            dialogNavigation.activate(DialogConfig.Qr)
        }
    }

    override fun onDialogDismissed() {
        dialogNavigation.dismiss()
    }

    private fun logout() {
        scope.launch {
            settingsRepository.putAccessToken(null)
            WebManager.clearWebViewCookies()
            unauthorizedError()
        }
    }

    @Serializable
    private sealed class DialogConfig {
        @Serializable
        data object Logout : DialogConfig()

        @Serializable
        data object Qr : DialogConfig()
    }

    private companion object {
        const val FORUM_URL = "https://4pda.to/forum/index.php?showtopic=1085976"
    }
}