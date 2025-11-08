package ru.slartus.boostbuddy.components.auth

data class AuthViewState(
    val url: String = AUTH_URL
) {
    companion object {
        internal const val AUTH_URL = "https://boosty.to"
    }
}