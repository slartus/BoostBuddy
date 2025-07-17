package ru.slartus.boostbuddy.components.settings

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.AppSettings
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.navigation.NavigationRouter
import ru.slartus.boostbuddy.navigation.NavigationTree
import ru.slartus.boostbuddy.navigation.navigateTo
import ru.slartus.boostbuddy.utils.BufferLoggingTracker
import ru.slartus.boostbuddy.utils.Platform
import ru.slartus.boostbuddy.utils.PlatformConfiguration

@Stable
interface SettingsComponent {
    val viewStates: Value<SettingsViewState>
    fun onUseSystemPlayerClicked(value: Boolean)
    fun onDebugLogClicked(value: Boolean)
    fun onDonateClicked()
    fun onVersionClicked()
    fun onSendLogClicked()
    fun onSetDarkModeClicked(value: Boolean)
}

data class SettingsViewState(
    val appSettings: AppSettings
)

internal class SettingsComponentImpl(
    componentContext: ComponentContext,
    val onVersionClickedHandler: () -> Unit
) : BaseComponent<SettingsViewState, Any>(componentContext, SettingsViewState(AppSettings.Default)),
    SettingsComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val platformConfiguration by Inject.lazy<PlatformConfiguration>()
    private val bufferLoggingTracker by Inject.lazy<BufferLoggingTracker>()
    private val navigationRouter by Inject.lazy<NavigationRouter>()

    init {
        subscribeSettings()
    }

    private fun subscribeSettings() {
        scope.launch {
            settingsRepository.appSettingsFlow.collect {
                viewState = viewState.copy(appSettings = it)
            }
        }
    }

    override fun onUseSystemPlayerClicked(value: Boolean) {
        scope.launch {
            settingsRepository.setUseSystemVideoPlayer(value)
        }
    }

    override fun onDebugLogClicked(value: Boolean) {
        scope.launch {
            settingsRepository.setDebugLog(value)
        }
    }

    override fun onDonateClicked() {
        when (platformConfiguration.platform) {
            Platform.Android,
            Platform.iOS -> platformConfiguration.openBrowser(
                url = DONATE_URL,
                onError = {
                    activateDonateQr()
                }
            )

            Platform.AndroidTV -> {
                activateDonateQr()
            }
        }
    }

    override fun onVersionClicked() {
        onVersionClickedHandler()
    }

    override fun onSendLogClicked() {
        scope.launch {
            runCatching {
                platformConfiguration.shareFile(bufferLoggingTracker.getLogPath())
            }.onFailure {
                Napier.e("onSendLogClicked", it)
            }
        }
    }

    override fun onSetDarkModeClicked(value: Boolean) {
        scope.launch {
            settingsRepository.setDarkMode(value)
        }
    }

    private fun activateDonateQr() {
        navigationRouter.navigateTo(NavigationTree.Qr("Поддержать проект", DONATE_URL))
    }

    private companion object {
        const val DONATE_URL = "https://yoomoney.ru/fundraise/13LKN6UNV5C.240629"
    }
}