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
import ru.slartus.boostbuddy.data.repositories.ProfileRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.models.AuthResponse

@Stable
interface AuthComponent {
    val viewStates: Value<AuthViewState>
    fun onCookiesChanged(cookies: String)
    fun onReloadClick()
}

internal class AuthComponentImpl(
    componentContext: ComponentContext,
    private val onLogined: () -> Unit,
) : BaseComponent<AuthViewState, Any>(componentContext, AuthViewState()), AuthComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val profileRepository by Inject.lazy<ProfileRepository>()
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
                val json = authCookie.value.decodeURLQueryComponent()
                Json.decodeFromString<AuthResponse>(json).accessToken?.let { accessToken ->
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
}