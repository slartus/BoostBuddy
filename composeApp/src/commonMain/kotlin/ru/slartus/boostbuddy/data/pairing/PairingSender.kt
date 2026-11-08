package ru.slartus.boostbuddy.data.pairing

internal interface PairingSender {
    suspend fun send(target: PairingTarget, sealed: String): Result<Unit>
}
