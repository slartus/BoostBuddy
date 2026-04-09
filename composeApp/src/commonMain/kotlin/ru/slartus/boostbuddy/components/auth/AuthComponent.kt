package ru.slartus.boostbuddy.components.auth

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import io.ktor.http.decodeURLQueryComponent
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.analytic.analytics
import ru.slartus.boostbuddy.data.log.logger
import ru.slartus.boostbuddy.data.repositories.PhoneAuthRepository
import ru.slartus.boostbuddy.data.repositories.ProfileRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.models.AuthResponse

@Stable
interface AuthComponent {
    val viewStates: Value<AuthViewState>
    fun onCookiesChanged(cookies: String)
    fun onReloadClick()
    fun onSwitchToPhoneLoginClick()
    fun onSwitchToWebViewClick()
    fun onPhoneChanged(value: String)
    fun onSmsCodeChanged(value: String)
    fun onSendSmsCodeClick()
    fun onResendSmsCodeClick()
    fun onConfirmSmsCodeClick()
    fun onBackToPhoneClick()
    fun onDismissError()
}

@Suppress("DEPRECATION")
internal class AuthComponentImpl(
    componentContext: ComponentContext,
    private val onLogined: () -> Unit,
) : BaseComponent<AuthViewState, Any>(componentContext, AuthViewState()), AuthComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val profileRepository by Inject.lazy<ProfileRepository>()
    private val phoneAuthRepository by Inject.lazy<PhoneAuthRepository>()
    private val json by Inject.lazy<Json>()
    private val badTokens = mutableSetOf<String>()
    private var checkTokenJob: Job = Job()

    init {
        clearToken()
    }

    private fun clearToken() {
        scope.launch {
            settingsRepository.putAccessToken(null)
        }
    }

    override fun onCookiesChanged(cookies: String) {
        scope.launch {
            runCatching {
                val authCookie =
                    parseCookies(cookies).entries.firstOrNull { it.key == "auth" } ?: return@launch
                val jsonObject = authCookie.value.decodeURLQueryComponent()
                json.decodeFromString<AuthResponse>(jsonObject).accessToken?.let { accessToken ->
                    if (accessToken != settingsRepository.getAccessToken() && accessToken !in badTokens) {
                        checkTokenJob.cancel()
                        checkTokenJob = launch(SupervisorJob()) {
                            checkToken(accessToken)
                        }
                    }
                }
            }.onFailure { logger.e(it, "onCookiesChanged") }
        }
    }

    override fun onReloadClick() {
        viewState =
            viewState.copy(url = "${AuthViewState.AUTH_URL}?${Clock.System.now().epochSeconds}")
    }

    override fun onSwitchToPhoneLoginClick() {
        viewState = viewState.copy(mode = AuthViewState.Mode.EnterPhone, errorMessage = null)
    }

    override fun onSwitchToWebViewClick() {
        viewState = viewState.copy(mode = AuthViewState.Mode.WebView, errorMessage = null)
    }

    override fun onPhoneChanged(value: String) {
        viewState = viewState.copy(phone = value)
    }

    override fun onSmsCodeChanged(value: String) {
        viewState = viewState.copy(smsCode = value)
    }

    override fun onBackToPhoneClick() {
        viewState = viewState.copy(
            mode = AuthViewState.Mode.EnterPhone,
            smsCode = "",
            challengeCode = null,
            errorMessage = null,
        )
    }

    override fun onDismissError() {
        viewState = viewState.copy(errorMessage = null)
    }

    override fun onSendSmsCodeClick() {
        requestCode(transport = null, switchToEnterCode = true)
    }

    override fun onResendSmsCodeClick() {
        val availableAt = viewState.resendAvailableAtEpochMs
        if (availableAt != null && Clock.System.now().toEpochMilliseconds() < availableAt) return
        requestCode(transport = "gate", switchToEnterCode = false)
    }

    private fun requestCode(transport: String?, switchToEnterCode: Boolean) {
        val phone = formatPhone(viewState.phone)
        if (phone == null) {
            viewState = viewState.copy(errorMessage = "Некорректный номер телефона")
            return
        }
        if (viewState.isLoading) return
        viewState = viewState.copy(isLoading = true, errorMessage = null)
        scope.launch {
            val deviceId = settingsRepository.getOrCreateDeviceId()
            phoneAuthRepository.sendSmsCode(
                deviceId = deviceId,
                phone = phone,
                transport = transport,
            )
                .onSuccess { challenge ->
                    val resendAt = Clock.System.now().toEpochMilliseconds() + 60 * 1000L
                    viewState = viewState.copy(
                        isLoading = false,
                        challengeCode = challenge.code,
                        resendAvailableAtEpochMs = resendAt,
                        mode = if (switchToEnterCode) AuthViewState.Mode.EnterSmsCode else viewState.mode,
                    )
                }
                .onFailure { t ->
                    logger.e(t, "sendSmsCode")
                    viewState = viewState.copy(
                        isLoading = false,
                        errorMessage = t.message ?: "Ошибка отправки SMS",
                    )
                }
        }
    }

    override fun onConfirmSmsCodeClick() {
        val state = viewState
        val phone = formatPhone(state.phone) ?: run {
            viewState = state.copy(errorMessage = "Некорректный номер телефона")
            return
        }
        val challenge = state.challengeCode ?: run {
            viewState = state.copy(errorMessage = "Запросите код повторно")
            return
        }
        val sms = state.smsCode.trim()
        if (sms.isEmpty()) {
            viewState = state.copy(errorMessage = "Введите код из SMS")
            return
        }
        if (state.isLoading) return
        viewState = state.copy(isLoading = true, errorMessage = null)
        scope.launch {
            val deviceId = settingsRepository.getOrCreateDeviceId()
            phoneAuthRepository.confirmSmsCode(
                deviceId = deviceId,
                phone = phone,
                challengeCode = challenge,
                smsCode = sms,
            )
                .onSuccess { tokens ->
                    settingsRepository.putAccessToken(tokens.accessToken)
                    if (profileRepository.getProfile().isSuccess) {
                        viewState = viewState.copy(isLoading = false)
                        onLogined()
                    } else {
                        badTokens.add(tokens.accessToken)
                        settingsRepository.putAccessToken(null)
                        viewState = viewState.copy(
                            isLoading = false,
                            errorMessage = "Не удалось получить профиль",
                        )
                    }
                }
                .onFailure { t ->
                    logger.e(t, "confirmSmsCode")
                    viewState = viewState.copy(
                        isLoading = false,
                        errorMessage = t.message ?: "Неверный код",
                    )
                }
        }
    }

    private suspend fun checkToken(token: String) {
        settingsRepository.putAccessToken(token)
        if (profileRepository.getProfile().isSuccess) {
            analytics.trackEvent("auth", mapOf("action" to "login"))
            onLogined()
        }
        else badTokens.add(token)
    }

    private fun parseCookies(cookies: String?): Map<String, String> {
        cookies ?: return emptyMap()

        return cookies.split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.split("=") }
            .associate { it[0] to it[1] }
    }

    /**
     * Boosty API expects phone in E.164 format: "+XXXXXXXXXXX".
     * Accepts user input with or without "+" and with arbitrary formatting.
     */
    private fun formatPhone(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        if (digits.length < 10) return null
        val full = if (digits.length == 10) "7$digits" else digits
        return "+$full"
    }
}
