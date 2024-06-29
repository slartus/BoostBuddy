package ru.slartus.boostbuddy.components.blog

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.AppSettings
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl

@Stable
interface VideoTypeComponent {
    val viewStates: Value<VideoTypeViewState>
    fun onDismissClicked()
    fun onItemClicked(playerUrl: PlayerUrl)
    fun onUseSystemPlayerClicked(value: Boolean)
}

data class VideoTypeViewState(
    val postData: Content.OkVideo,
    val useSystemPlayer: Boolean
)

class VideoTypeComponentImpl(
    componentContext: ComponentContext,
    postData: Content.OkVideo,
    private val onDismissed: () -> Unit,
    private val onItemClicked: (PlayerUrl) -> Unit,
) : BaseComponent<VideoTypeViewState, Any>(
    componentContext,
    VideoTypeViewState(postData, AppSettings.Default.useSystemVideoPlayer)
), VideoTypeComponent {

    private val settingsRepository by Inject.lazy<SettingsRepository>()

    init {
        subscribeSettings()
    }

    private fun subscribeSettings() {
        scope.launch {
            settingsRepository.appSettingsFlow.collect {
                viewState = viewState.copy(useSystemPlayer = it.useSystemVideoPlayer)
            }
        }
    }

    override fun onDismissClicked() {
        onDismissed()
    }

    override fun onItemClicked(playerUrl: PlayerUrl) {
        onItemClicked.invoke(playerUrl)
    }

    override fun onUseSystemPlayerClicked(value: Boolean) {
        scope.launch {
            settingsRepository.setUseSystemVideoPlayer(value)
        }
    }

}