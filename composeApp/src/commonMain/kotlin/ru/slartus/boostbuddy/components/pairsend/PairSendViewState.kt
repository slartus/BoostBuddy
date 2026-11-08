package ru.slartus.boostbuddy.components.pairsend

sealed class PairSendViewState {
    data class Confirm(val host: String) : PairSendViewState()
    data object Sending : PairSendViewState()
    data object Done : PairSendViewState()
    data class Error(val kind: PairSendErrorKind) : PairSendViewState()
}

enum class PairSendErrorKind {
    NotLoggedIn,
    Network,
    Rejected,
    Uncertain,
    Unknown,
}
