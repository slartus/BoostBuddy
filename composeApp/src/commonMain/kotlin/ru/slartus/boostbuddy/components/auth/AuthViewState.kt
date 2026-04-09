package ru.slartus.boostbuddy.components.auth

data class AuthViewState(
    val url: String = AUTH_URL,
    val mode: Mode = Mode.WebView,
    val phone: String = "",
    val smsCode: String = "",
    val challengeCode: String? = null,
    val resendAvailableAtEpochMs: Long? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    enum class Mode { WebView, EnterPhone, EnterSmsCode }

    companion object {
        internal const val AUTH_URL = "https://boosty.to"
    }
}
