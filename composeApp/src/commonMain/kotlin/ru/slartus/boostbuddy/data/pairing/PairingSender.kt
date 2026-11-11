package ru.slartus.boostbuddy.data.pairing

internal interface PairingSender {
    suspend fun send(target: PairingTarget, sealed: String): Result<Unit>
}

internal sealed class PairingSenderException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    class InvalidTarget : PairingSenderException("Pairing target is not a private LAN address")
    class NotDelivered(cause: Throwable) : PairingSenderException("Pairing request was not delivered", cause)
    class Uncertain(cause: Throwable) : PairingSenderException("Pairing request outcome is unknown", cause)
}
