package ru.slartus.boostbuddy.components.qrlogin

import ru.slartus.boostbuddy.data.ktor.BoostyTokenRefresher
import ru.slartus.boostbuddy.data.repositories.AuthSession
import ru.slartus.boostbuddy.data.repositories.Profile
import ru.slartus.boostbuddy.data.repositories.ProfileRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository

internal interface QrLoginSessionGateway {
    suspend fun currentSession(): AuthSession
    suspend fun replaceSession(session: AuthSession)
    suspend fun fetchProfileWithoutRefresh(): Result<Profile>
    suspend fun refreshSession(): Boolean
}

internal class DefaultQrLoginSessionGateway(
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val tokenRefresher: BoostyTokenRefresher,
) : QrLoginSessionGateway {
    override suspend fun currentSession(): AuthSession = settingsRepository.getSession()

    override suspend fun replaceSession(session: AuthSession) = settingsRepository.replaceSession(session)

    override suspend fun fetchProfileWithoutRefresh(): Result<Profile> =
        profileRepository.getProfile(allowTokenRefresh = false)

    override suspend fun refreshSession(): Boolean = tokenRefresher.forceRefresh() != null
}
