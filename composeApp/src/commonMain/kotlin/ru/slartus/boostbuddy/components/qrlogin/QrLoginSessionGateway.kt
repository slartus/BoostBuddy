package ru.slartus.boostbuddy.components.qrlogin

import ru.slartus.boostbuddy.data.ktor.BoostyTokenRefresher
import ru.slartus.boostbuddy.data.ktor.TokenRefreshOutcome
import ru.slartus.boostbuddy.data.repositories.AuthSession
import ru.slartus.boostbuddy.data.repositories.Profile
import ru.slartus.boostbuddy.data.repositories.ProfileRepository

internal enum class QrLoginCommitResult { Committed, Rejected, NetworkError }

internal interface QrLoginSessionGateway {
    suspend fun beginPendingSession(session: AuthSession)
    suspend fun rollbackPendingSession()
    suspend fun fetchProfileWithoutRefresh(): Result<Profile>
    suspend fun commitPendingSession(): QrLoginCommitResult
}

internal class DefaultQrLoginSessionGateway(
    private val profileRepository: ProfileRepository,
    private val tokenRefresher: BoostyTokenRefresher,
) : QrLoginSessionGateway {
    override suspend fun beginPendingSession(session: AuthSession) =
        tokenRefresher.beginPendingSession(session)

    override suspend fun rollbackPendingSession() =
        tokenRefresher.rollbackPendingSession()

    override suspend fun fetchProfileWithoutRefresh(): Result<Profile> =
        profileRepository.getProfile(allowTokenRefresh = false)

    override suspend fun commitPendingSession(): QrLoginCommitResult =
        when (tokenRefresher.commitPendingSession()) {
            is TokenRefreshOutcome.Success -> QrLoginCommitResult.Committed
            TokenRefreshOutcome.Rejected -> QrLoginCommitResult.Rejected
            TokenRefreshOutcome.NetworkError -> QrLoginCommitResult.NetworkError
        }
}
