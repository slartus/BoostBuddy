package ru.slartus.boostbuddy.components.qrlogin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ru.slartus.boostbuddy.data.log.logger
import ru.slartus.boostbuddy.data.pairing.PairingPayload
import ru.slartus.boostbuddy.data.pairing.PairingReceiver
import ru.slartus.boostbuddy.data.pairing.PairingReceiverEvent
import ru.slartus.boostbuddy.data.pairing.PairingReceiverException
import ru.slartus.boostbuddy.data.pairing.PairingSession
import ru.slartus.boostbuddy.data.repositories.AuthSession

internal class QrLoginController(
    private val scope: CoroutineScope,
    private val appScope: CoroutineScope,
    receiverProvider: () -> PairingReceiver,
    private val gateway: QrLoginSessionGateway,
    private val nowMs: () -> Long,
    private val onLogined: () -> Unit,
) {
    private val receiver by lazy(receiverProvider)
    private val _state = MutableStateFlow<QrLoginViewState>(QrLoginViewState.Starting)
    val state: StateFlow<QrLoginViewState> = _state.asStateFlow()

    private var isStarted = false
    private var isCommitting = false
    private var isLeaving = false
    private var isDestroyed = false
    private var hasPendingSession = false
    private var receiverUsed = false
    private var listenJob: Job? = null
    private var acceptJob: Job? = null

    fun onStart() {
        isStarted = true
        val current = state.value
        if (current is QrLoginViewState.Starting || current is QrLoginViewState.ShowQr) startListening()
    }

    fun onStop() {
        isStarted = false
        stopListening()
        if (state.value is QrLoginViewState.ShowQr) _state.value = QrLoginViewState.Starting
    }

    fun onDestroy() {
        isDestroyed = true
        stopListening()
        if (isCommitting) return
        val accept = acceptJob
        appScope.launch {
            accept?.cancelAndJoin()
            rollback()
        }
    }

    fun onRefreshQr() {
        val current = state.value
        if (current is QrLoginViewState.ShowQr || current is QrLoginViewState.Error) restartQr()
    }

    fun onRetry() {
        val current = state.value as? QrLoginViewState.Error ?: return
        if (current.kind == QrLoginErrorKind.RefreshNetwork && hasPendingSession) commit() else restartQr()
    }

    fun onConfirm() {
        if (state.value is QrLoginViewState.Confirm) commit()
    }

    private fun commit() {
        if (!hasPendingSession || isCommitting) return
        isCommitting = true
        _state.value = QrLoginViewState.Completing
        scope.launch {
            val result = withContext(NonCancellable) {
                gateway.commitPendingSession().also { result ->
                    when (result) {
                        QrLoginCommitResult.Committed -> hasPendingSession = false
                        QrLoginCommitResult.Rejected -> rollback()
                        QrLoginCommitResult.NetworkError -> if (isDestroyed) rollback()
                    }
                }
            }
            isCommitting = false
            when (result) {
                QrLoginCommitResult.Committed -> {
                    _state.value = QrLoginViewState.Success
                    onLogined()
                }

                QrLoginCommitResult.Rejected ->
                    _state.value = QrLoginViewState.Error(QrLoginErrorKind.RefreshRejected)

                QrLoginCommitResult.NetworkError ->
                    _state.value = QrLoginViewState.Error(QrLoginErrorKind.RefreshNetwork)
            }
        }
    }

    fun onCancel() {
        if (state.value is QrLoginViewState.Confirm) restartQr()
    }

    fun onBack(navigate: () -> Unit) {
        if (isCommitting || isLeaving) return
        isLeaving = true
        stopListening()
        val accept = acceptJob
        scope.launch {
            accept?.cancelAndJoin()
            rollback()
            navigate()
        }
    }

    private fun restartQr() {
        _state.value = QrLoginViewState.Starting
        scope.launch {
            rollback()
            if (isStarted) startListening()
        }
    }

    private fun startListening() {
        listenJob?.cancel()
        _state.value = QrLoginViewState.Starting
        receiverUsed = true
        listenJob = scope.launch {
            val session = receiver.start().getOrElse { error ->
                handleStartFailure(error)
                return@launch
            }
            when (val event = awaitEvent(session)) {
                is PairingReceiverEvent.Received -> {
                    acceptJob = scope.launch { accept(event.payload) }
                }

                is PairingReceiverEvent.TooManyAttempts ->
                    _state.value = QrLoginViewState.Error(QrLoginErrorKind.TooManyAttempts)

                is PairingReceiverEvent.Expired, null -> {
                    receiver.stop()
                    _state.value = QrLoginViewState.Error(QrLoginErrorKind.Expired)
                }
            }
        }
    }

    private fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        if (receiverUsed) receiver.stop()
    }

    private fun handleStartFailure(error: Throwable) {
        val kind = when (error) {
            is PairingReceiverException.Stopped -> return
            is PairingReceiverException.NoLan -> QrLoginErrorKind.NoLan
            is PairingReceiverException.NoFreePort -> QrLoginErrorKind.NoFreePort
            else -> QrLoginErrorKind.StartFailed
        }
        logger.e(error, "QrLogin: receiver start failed")
        _state.value = QrLoginViewState.Error(kind)
    }

    private suspend fun awaitEvent(session: PairingSession): PairingReceiverEvent? = coroutineScope {
        val ticker = launch { tick(session) }
        try {
            withTimeoutOrNull(remainingMs(session).coerceAtLeast(0)) {
                receiver.events.first { it.sid == session.sid }
            }
        } finally {
            ticker.cancel()
        }
    }

    private suspend fun tick(session: PairingSession) {
        while (true) {
            val remaining = remainingMs(session)
            val secondsLeft = ((remaining + MILLIS_IN_SECOND - 1) / MILLIS_IN_SECOND).coerceAtLeast(0).toInt()
            _state.value = QrLoginViewState.ShowQr(
                pageUrl = session.pageUrl,
                host = session.host,
                secondsLeft = secondsLeft,
            )
            if (remaining <= 0) return
            val toNextSecond = remaining % MILLIS_IN_SECOND
            delay(if (toNextSecond == 0L) MILLIS_IN_SECOND else toNextSecond)
        }
    }

    private fun remainingMs(session: PairingSession): Long = session.expiresAtMs - nowMs()

    private suspend fun accept(payload: PairingPayload) {
        _state.value = QrLoginViewState.Completing
        withContext(NonCancellable) {
            hasPendingSession = true
            gateway.beginPendingSession(
                AuthSession(
                    accessToken = payload.accessToken,
                    refreshToken = payload.refreshToken,
                    expiresAt = payload.expiresAt,
                    deviceId = payload.deviceId,
                )
            )
        }
        val profile = gateway.fetchProfileWithoutRefresh()
        currentCoroutineContext().ensureActive()
        profile
            .onSuccess {
                _state.value = QrLoginViewState.Confirm(profileName = it.name, avatarUrl = it.avatarUrl)
            }
            .onFailure {
                rollback()
                _state.value = QrLoginViewState.Error(QrLoginErrorKind.ProfileFailed)
            }
    }

    private suspend fun rollback() {
        if (!hasPendingSession) return
        hasPendingSession = false
        withContext(NonCancellable) { gateway.rollbackPendingSession() }
    }

    private companion object {
        const val MILLIS_IN_SECOND = 1000L
    }
}
