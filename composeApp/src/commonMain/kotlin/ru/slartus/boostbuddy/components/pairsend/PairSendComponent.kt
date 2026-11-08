@file:OptIn(ExperimentalTime::class)

package ru.slartus.boostbuddy.components.pairsend

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.pairing.PairingSender
import ru.slartus.boostbuddy.data.pairing.PairingTarget
import ru.slartus.boostbuddy.data.repositories.ProfileRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Stable
interface PairSendComponent {
    val viewStates: Value<PairSendViewState>
    fun onSend()
    fun onCancel()
    fun onRetry()
}

internal class PairSendComponentImpl(
    componentContext: ComponentContext,
    target: PairingTarget,
    private val onTransferred: () -> Unit,
    private val onCancelled: () -> Unit,
) : BaseComponent<PairSendViewState, Any>(componentContext, PairSendViewState.Confirm(target.host)),
    PairSendComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val profileRepository by Inject.lazy<ProfileRepository>()
    private val pairingSender by Inject.lazy<PairingSender>()

    private val controller = PairSendController(
        scope = scope,
        target = target,
        gateway = DefaultPairSendSessionGateway(
            settingsRepository = settingsRepository,
            profileRepository = profileRepository,
        ),
        senderProvider = { pairingSender },
        nowSeconds = { Clock.System.now().epochSeconds },
        onTransferred = { onTransferred() },
    )

    init {
        scope.launch {
            controller.state.collect { viewState = it }
        }
    }

    override fun onSend() = controller.onSend()

    override fun onCancel() {
        if (controller.canLeave()) onCancelled()
    }

    override fun onRetry() = controller.onRetry()
}
