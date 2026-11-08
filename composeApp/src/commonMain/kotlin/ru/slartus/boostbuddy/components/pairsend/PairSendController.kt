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
            val session = when (val check = gateway.loadValidSession()) {
                is PairSendSessionCheck.Valid -> check.session
                PairSendSessionCheck.NotLoggedIn -> return@launch fail(PairSendErrorKind.NotLoggedIn)
                PairSendSessionCheck.Network -> return@launch fail(PairSendErrorKind.Network)
            }
            val sealed = withContext(Dispatchers.Default) { seal(session) }
            val result = withContext(NonCancellable) {
                sender.send(target, sealed).onSuccess { gateway.clearLocalSession() }
            }
            result
                .onSuccess {
                    _state.value = PairSendViewState.Done
                    onTransferred()
                }
                .onFailure { error ->
                    logger.e(error, "PairSend: send failed")
                    fail(error.toErrorKind())
                }
        }
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
            HttpStatusCode.Gone, HttpStatusCode.BadRequest -> PairSendErrorKind.Rejected
            else -> PairSendErrorKind.Unknown
        }

        is ServerException.Other -> PairSendErrorKind.Network
        else -> PairSendErrorKind.Unknown
    }
}
