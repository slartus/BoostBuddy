@file:OptIn(ExperimentalTime::class)

package ru.slartus.boostbuddy.components.qrlogin

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnStart
import com.arkivanov.essenty.lifecycle.doOnStop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.ktor.BoostyTokenRefresher
import ru.slartus.boostbuddy.data.pairing.PairingReceiver
import ru.slartus.boostbuddy.data.repositories.ProfileRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Stable
interface QrLoginComponent {
    val viewStates: Value<QrLoginViewState>
    fun onRefreshQr()
    fun onConfirm()
    fun onCancel()
    fun onBack()
    fun onOtherMethod()
    fun onRetry()
}

internal class QrLoginComponentImpl(
    componentContext: ComponentContext,
    private val onLogined: () -> Unit,
    private val onBackClicked: () -> Unit,
    private val onOtherMethodClicked: () -> Unit,
) : BaseComponent<QrLoginViewState, Any>(componentContext, QrLoginViewState.Starting), QrLoginComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val profileRepository by Inject.lazy<ProfileRepository>()
    private val tokenRefresher by Inject.lazy<BoostyTokenRefresher>()
    private val pairingReceiver by Inject.lazy<PairingReceiver>()
    private val appScope by Inject.lazy<CoroutineScope>()

    private val controller = QrLoginController(
        scope = scope,
        appScope = appScope,
        receiverProvider = { pairingReceiver },
        gateway = DefaultQrLoginSessionGateway(
            settingsRepository = settingsRepository,
            profileRepository = profileRepository,
            tokenRefresher = tokenRefresher,
        ),
        nowMs = { Clock.System.now().toEpochMilliseconds() },
        onLogined = { onLogined() },
    )

    init {
        scope.launch {
            controller.state.collect { viewState = it }
        }
        lifecycle.doOnStart { controller.onStart() }
        lifecycle.doOnStop { controller.onStop() }
        lifecycle.doOnDestroy { controller.onDestroy() }
        backHandler.register(BackCallback { onBack() })
    }

    override fun onRefreshQr() = controller.onRefreshQr()

    override fun onConfirm() = controller.onConfirm()

    override fun onCancel() = controller.onCancel()

    override fun onBack() = controller.onBack(onBackClicked)

    override fun onOtherMethod() = controller.onBack(onOtherMethodClicked)

    override fun onRetry() = controller.onRetry()
}
