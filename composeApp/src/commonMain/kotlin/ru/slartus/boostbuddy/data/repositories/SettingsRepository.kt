package ru.slartus.boostbuddy.data.repositories

import com.russhwolf.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SettingsRepository(
    private val settings: Settings
) {
    private val tokenBus: MutableStateFlow<String?> = MutableStateFlow(
        null
    )

    val tokenFlow: StateFlow<String?>
        get() = tokenBus.asStateFlow()

    init {
        tokenBus.value = settings.getStringOrNull("access_token")
    }

    suspend fun getString(key: String): String? = withContext(Dispatchers.IO) {
        settings.getStringOrNull(key)
    }

    suspend fun putString(key: String, value: String) = withContext(Dispatchers.IO) {
        settings.putString(key, value)
    }


    suspend fun putAccessToken(value: String?) {
        if (value == null)
            settings.remove("access_token")
        else
            settings.putString("access_token", value)
        tokenBus.value = value
    }

    suspend fun getAccessToken(): String? = getString("access_token")
}

