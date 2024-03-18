package ru.slartus.boostbuddy.components.auth

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import io.ktor.http.decodeURLQueryComponent
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.AuthResponse
import ru.slartus.boostbuddy.data.repositories.SettingsRepository

interface AuthComponent {
    fun onCookiesChanged(cookies: String)
}

class AuthComponentImpl(
    componentContext: ComponentContext,
    private val onLogined: () -> Unit,
) : BaseComponent<Unit, Any>(componentContext, Unit), AuthComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()

    init {
        clearToken()
        backHandler.register(object : BackCallback() {
            override fun onBack() {

            }
        })
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
                val auth = Json.decodeFromString<AuthResponse>(json)
                if (auth.accessToken != null && auth.accessToken != settingsRepository.getAccessToken()) {
                    settingsRepository.putAccessToken(auth.accessToken)
                    onLogined()
                }
            }.onFailure { it.printStackTrace() }
        }
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