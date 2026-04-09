package ru.slartus.boostbuddy.data.repositories

import ru.slartus.boostbuddy.data.api.PhoneAuthApi
import ru.slartus.boostbuddy.utils.fetchOrError

internal class PhoneAuthRepository(
    private val phoneAuthApi: PhoneAuthApi,
) {
    suspend fun sendSmsCode(
        deviceId: String,
        phone: String,
        transport: String?,
    ): Result<PhoneCodeChallengeInfo> = fetchOrError {
        val response = phoneAuthApi.sendPhoneVerificationCode(
            deviceId = deviceId,
            phone = phone,
            transport = transport,
        )
        val challenge = response.data?.phoneCode ?: error("empty phoneCode")
        PhoneCodeChallengeInfo(
            code = challenge.code ?: error("empty code"),
            sentTransport = challenge.sentTransport,
            expiresInSeconds = (challenge.expiresIn ?: 0L) / 1000,
        )
    }

    suspend fun confirmSmsCode(
        deviceId: String,
        phone: String,
        challengeCode: String,
        smsCode: String,
    ): Result<PhoneAuthTokens> = fetchOrError {
        val response = phoneAuthApi.confirmPhoneVerificationCode(
            deviceId = deviceId,
            phone = phone,
            challengeCode = challengeCode,
            smsCode = smsCode,
        )
        PhoneAuthTokens(
            accessToken = response.accessToken ?: error("empty access_token"),
            refreshToken = response.refreshToken.orEmpty(),
            expiresIn = response.expiresIn ?: 0L,
        )
    }
}

internal data class PhoneCodeChallengeInfo(
    val code: String,
    val sentTransport: String?,
    val expiresInSeconds: Long,
)

internal data class PhoneAuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)
