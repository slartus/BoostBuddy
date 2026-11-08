package ru.slartus.boostbuddy.components.pairsend

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.slartus.boostbuddy.data.log.logger
import ru.slartus.boostbuddy.data.pairing.PairingEnvelope
import ru.slartus.boostbuddy.data.pairing.PairingPayload
import ru.slartus.boostbuddy.data.pairing.PairingSender
import ru.slartus.boostbuddy.data.pairing.PairingSenderException
import ru.slartus.boostbuddy.data.pairing.PairingTarget
import ru.slartus.boostbuddy.data.repositories.AuthSession
import ru.slartus.boostbuddy.utils.ServerException

internal class PairSendController(
    private val scope: CoroutineScope,
    private val target: PairingTarget,
    private val gateway: PairSendSessionGateway,
    senderProvider: () -> PairingSender,
    private val nowSeconds: () -> Long,
    private val onTransferred: () -> Unit,
) {
    private val sender by lazy(senderProvider)
    private val _state = MutableStateFlow<PairSendViewState>(PairSendViewState.Confirm(target.host))
    val state: StateFlow<PairSendViewState> = _state.asStateFlow()

    fun onSend() {
        val current = state.value
        if (current is PairSendViewState.Confirm || current is PairSendViewState.Error) send()
    }

    fun onRetry() {
        if (state.value is PairSendViewState.Error) send()
    }

    fun canLeave(): Boolean = state.value !is PairSendViewState.Sending

    private fun send() {
        _state.value = PairSendViewState.Sending
        scope.launch {
            when (gateway.checkSession()) {
                PairSendSessionCheck.Valid -> Unit
                PairSendSessionCheck.NotLoggedIn -> return@launch fail(PairSendErrorKind.NotLoggedIn)
                PairSendSessionCheck.Network -> return@launch fail(PairSendErrorKind.Network)
            }
            val outcome = withContext(NonCancellable) {
                gateway.withExclusiveSession { session -> transfer(session) }
            }
            when (outcome) {
                SendOutcome.Done -> {
                    _state.value = PairSendViewState.Done
                    onTransferred()
                }

                SendOutcome.NotLoggedIn -> fail(PairSendErrorKind.NotLoggedIn)
                is SendOutcome.Failed -> {
                    logger.e(outcome.error, "PairSend: send failed")
                    fail(outcome.error.toErrorKind())
                }
            }
        }
    }

    private suspend fun transfer(session: AuthSession): SendOutcome {
        if (session.accessToken.isNullOrEmpty() || session.refreshToken.isNullOrEmpty()) {
            return SendOutcome.NotLoggedIn
        }
        val sealed = withContext(Dispatchers.Default) { seal(session) }
        return sender.send(target, sealed).fold(
            onSuccess = {
                gateway.clearLocalSession()
                SendOutcome.Done
            },
            onFailure = { SendOutcome.Failed(it) },
        )
    }

    private fun fail(kind: PairSendErrorKind) {
        _state.value = PairSendViewState.Error(kind)
    }

    private fun seal(session: AuthSession): String {
        val payload = PairingPayload(
            accessToken = session.accessToken.orEmpty(),
            refreshToken = session.refreshToken.orEmpty(),
            expiresAt = session.expiresAt,
            deviceId = session.deviceId.orEmpty(),
            source = PairingPayload.SOURCE_APP,
            iat = nowSeconds(),
        )
        val plain = Json.encodeToString(payload).encodeToByteArray()
        return try {
            PairingEnvelope.seal(target.key, target.sid, plain)
        } finally {
            plain.fill(0)
        }
    }

    private fun Throwable.toErrorKind(): PairSendErrorKind = when (this) {
        is ServerException.Request -> when (code) {
            HttpStatusCode.Gone, HttpStatusCode.BadRequest, HttpStatusCode.NotFound -> PairSendErrorKind.Rejected
            else -> PairSendErrorKind.Unknown
        }

        is PairingSenderException.InvalidTarget -> PairSendErrorKind.Rejected
        is PairingSenderException.NotDelivered -> PairSendErrorKind.Network
        is PairingSenderException.Uncertain -> PairSendErrorKind.Uncertain
        else -> PairSendErrorKind.Unknown
    }

    private sealed class SendOutcome {
        data object Done : SendOutcome()
        data object NotLoggedIn : SendOutcome()
        data class Failed(val error: Throwable) : SendOutcome()
    }
}
