package ru.slartus.boostbuddy.components.auth

import com.arkivanov.decompose.ComponentContext
import io.github.aakira.napier.Napier
import io.ktor.http.decodeURLQueryComponent
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.ProfileRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.models.AuthResponse

interface AuthComponent {
    fun onCookiesChanged(cookies: String)
}

class AuthComponentImpl(
    componentContext: ComponentContext,
    private val onLogined: () -> Unit,
) : BaseComponent<Unit, Any>(componentContext, Unit), AuthComponent {
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
            }.onFailure { Napier.e("onCookiesChanged", it) }
        }
    }

    private suspend fun checkToken(token: String) {
        settingsRepository.putAccessToken(token)
        if (profileRepository.getProfile().isSuccess) onLogined()
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