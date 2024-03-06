package ru.slartus.boostbuddy.components

import com.arkivanov.decompose.ComponentContext
import io.ktor.http.decodeURLQueryComponent
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ru.slartus.boostbuddy.data.repositories.AuthResponse


interface AuthComponent {
    fun onCookiesChanged(cookies: String)
}

class AuthComponentImpl(
    componentContext: ComponentContext,
    private val onLogined: () -> Unit,
) : AuthComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()
    private var checkCookiesJob: Job? = null

    override fun onCookiesChanged(cookies: String) {
        checkCookiesJob?.cancel()
        checkCookiesJob = scope.launch {
            runCatching {
                val authCookie =
                    parseCookies(cookies).entries.firstOrNull { it.key == "auth" } ?: return@launch
                val json = authCookie.value.decodeURLQueryComponent()
                val auth = Json.decodeFromString<AuthResponse>(json)
                if (auth.accessToken != null && auth.refreshToken != null) {
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