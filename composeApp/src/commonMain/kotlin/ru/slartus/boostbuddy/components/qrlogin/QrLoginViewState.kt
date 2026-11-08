package ru.slartus.boostbuddy.components.qrlogin

sealed class QrLoginViewState {
    data object Starting : QrLoginViewState()

    data class ShowQr(
        val pageUrl: String,
        val host: String,
        val secondsLeft: Int,
    ) : QrLoginViewState() {
        override fun toString(): String = "ShowQr(host=$host, secondsLeft=$secondsLeft)"
    }

    data class Confirm(
        val profileName: String?,
        val avatarUrl: String?,
    ) : QrLoginViewState()

    data object Completing : QrLoginViewState()

    data object Success : QrLoginViewState()

    data class Error(val kind: QrLoginErrorKind) : QrLoginViewState()
}

enum class QrLoginErrorKind {
    NoLan,
    NoFreePort,
    StartFailed,
    Expired,
    TooManyAttempts,
    ProfileFailed,
    RefreshFailed,
}
