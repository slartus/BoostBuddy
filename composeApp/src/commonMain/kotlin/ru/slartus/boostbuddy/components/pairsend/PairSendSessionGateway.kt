package ru.slartus.boostbuddy.components.pairsend

import kotlinx.coroutines.CancellationException
import ru.slartus.boostbuddy.data.ktor.BoostyTokenRefresher
import ru.slartus.boostbuddy.data.log.logger
import ru.slartus.boostbuddy.data.repositories.AuthSession
import ru.slartus.boostbuddy.data.repositories.ProfileRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.utils.ServerException
import ru.slartus.boostbuddy.utils.WebManager

internal enum class PairSendSessionCheck { Valid, NotLoggedIn, Network }

internal interface PairSendSessionGateway {
    suspend fun checkSession(): PairSendSessionCheck
    suspend fun <T> withExclusiveSession(block: suspend (AuthSession) -> T): T
    suspend fun clearLocalSession()
}

internal class DefaultPairSendSessionGateway(
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val tokenRefresher: BoostyTokenRefresher,
) : PairSendSessionGateway {
    override suspend fun checkSession(): PairSendSessionCheck {
        if (settingsRepository.getAccessToken().isNullOrEmpty()) return PairSendSessionCheck.NotLoggedIn
        val error = profileRepository.getProfile().exceptionOrNull() ?: return PairSendSessionCheck.Valid
        return if (error is ServerException.Unauthorized) {
            PairSendSessionCheck.NotLoggedIn
        } else {
            PairSendSessionCheck.Network
        }
    }

    override suspend fun <T> withExclusiveSession(block: suspend (AuthSession) -> T): T =
        tokenRefresher.withExclusiveSession { session ->
            block(session.copy(deviceId = session.deviceId ?: settingsRepository.getOrCreateDeviceId()))
        }

    override suspend fun clearLocalSession() {
        settingsRepository.clearSessionWithNewDeviceId()
        try {
            WebManager.clearWebViewCookies()
            WebManager.clearWebViewStorage()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e, "PairSend: failed to clear WebView data")
        }
    }
}
