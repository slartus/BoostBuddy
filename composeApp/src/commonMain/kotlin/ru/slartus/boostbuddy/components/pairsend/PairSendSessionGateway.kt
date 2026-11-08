package ru.slartus.boostbuddy.components.pairsend

import kotlinx.coroutines.CancellationException
import ru.slartus.boostbuddy.data.log.logger
import ru.slartus.boostbuddy.data.repositories.AuthSession
import ru.slartus.boostbuddy.data.repositories.ProfileRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.utils.ServerException
import ru.slartus.boostbuddy.utils.WebManager

internal sealed class PairSendSessionCheck {
    data class Valid(val session: AuthSession) : PairSendSessionCheck()
    data object NotLoggedIn : PairSendSessionCheck()
    data object Network : PairSendSessionCheck()
}

internal interface PairSendSessionGateway {
    suspend fun loadValidSession(): PairSendSessionCheck
    suspend fun clearLocalSession()
}

internal class DefaultPairSendSessionGateway(
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
) : PairSendSessionGateway {
    override suspend fun loadValidSession(): PairSendSessionCheck {
        if (settingsRepository.getAccessToken().isNullOrEmpty()) return PairSendSessionCheck.NotLoggedIn
        profileRepository.getProfile().exceptionOrNull()?.let { error ->
            return if (error is ServerException.Unauthorized) {
                PairSendSessionCheck.NotLoggedIn
            } else {
                PairSendSessionCheck.Network
            }
        }
        val session = settingsRepository.getSession()
        if (session.accessToken.isNullOrEmpty() || session.refreshToken.isNullOrEmpty()) {
            return PairSendSessionCheck.NotLoggedIn
        }
        val deviceId = session.deviceId ?: settingsRepository.getOrCreateDeviceId()
        return PairSendSessionCheck.Valid(session.copy(deviceId = deviceId))
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
