package ru.slartus.boostbuddy.components.settings

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.AppSettings
import ru.slartus.boostbuddy.data.repositories.SettingsRepository

@Stable
interface SettingsComponent {
    val viewStates: Value<SettingsViewState>
    fun onUseSystemPlayerClicked(value: Boolean)
}

data class SettingsViewState(
    val appSettings: AppSettings
)

internal class SettingsComponentImpl(
    componentContext: ComponentContext,
) : BaseComponent<SettingsViewState, Any>(componentContext, SettingsViewState(AppSettings.Default)),
    SettingsComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()

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
}