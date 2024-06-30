package ru.slartus.boostbuddy.components.settings

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.AppSettings
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.utils.Platform
import ru.slartus.boostbuddy.utils.PlatformConfiguration

@Stable
interface SettingsComponent {
    val viewStates: Value<SettingsViewState>
    val dialogSlot: Value<ChildSlot<*, DialogChild>>
    fun onUseSystemPlayerClicked(value: Boolean)
    fun onDonateClicked()
    fun onDialogDismissed()
    fun onVersionClicked()

    sealed class DialogChild {
        data class Qr(val title: String, val url: String) : DialogChild()
    }
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
    private val dialogNavigation = SlotNavigation<DialogConfig>()

    override val dialogSlot: Value<ChildSlot<*, SettingsComponent.DialogChild>> =
        childSlot(
            key = "dialogSlot",
            source = dialogNavigation,
            serializer = DialogConfig.serializer(),
            handleBackButton = true,
            childFactory = ::dialogChild
        )

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

    override fun onDialogDismissed() {
        dialogNavigation.dismiss()
    }

    override fun onVersionClicked() {
        onVersionClickedHandler()
    }

    private fun activateDonateQr() {
        dialogNavigation.activate(DialogConfig.Qr("Поддержать проект", DONATE_URL))
    }

    private fun dialogChild(
        config: DialogConfig,
        componentContext: ComponentContext
    ): SettingsComponent.DialogChild =
        when (config) {
            is DialogConfig.Qr -> SettingsComponent.DialogChild.Qr(
                title = config.title,
                url = config.url
            )
        }

    @Serializable
    private sealed class DialogConfig {
        @Serializable
        data class Qr(val title: String, val url: String) : DialogConfig()
    }

    private companion object {
        const val DONATE_URL = "https://yoomoney.ru/fundraise/13LKN6UNV5C.240629"
    }
}