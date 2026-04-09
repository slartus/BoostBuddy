package ru.slartus.boostbuddy.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.formUrlEncode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private val FORM_URL_ENCODED = ContentType.Application.FormUrlEncoded

internal class PhoneAuthApi(
    private val httpClient: HttpClient,
) {
    suspend fun sendPhoneVerificationCode(
        deviceId: String,
        phone: String,
        transport: String?,
    ): PhoneSendCodeResponse = httpClient.post("auth/phone/verification_code/send") {
        header("X-From-Id", deviceId)
        headers.remove(HttpHeaders.AcceptCharset)
        val params = buildList {
            add("phone" to phone)
            add("device_os" to "web")
            add("device_id" to deviceId)
            if (transport != null) add("transport" to transport)
        }
        setBody(TextContent(params.formUrlEncode(), FORM_URL_ENCODED))
    }.body()

    suspend fun confirmPhoneVerificationCode(
        deviceId: String,
        phone: String,
        challengeCode: String,
        smsCode: String,
    ): PhoneConfirmCodeResponse = httpClient.put("auth/phone/verification_code/confirm") {
        header("X-From-Id", deviceId)
        headers.remove(HttpHeaders.AcceptCharset)
        val params = listOf(
            "phone" to phone,
            "code" to challengeCode,
            "sms_code" to smsCode,
            "device_os" to "web",
            "device_id" to deviceId,
        )
        setBody(TextContent(params.formUrlEncode(), FORM_URL_ENCODED))
    }.body()
}

@Serializable
internal data class PhoneSendCodeResponse(
    val data: PhoneSendCodeData? = null,
)

@Serializable
internal data class PhoneSendCodeData(
    val phoneCode: PhoneCodeChallenge? = null,
)

@Serializable
internal data class PhoneCodeChallenge(
    val code: String? = null,
    val sentTransport: String? = null,
    val expiresIn: Long? = null,
    val phone: String? = null,
)

@Serializable
internal data class PhoneConfirmCodeResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
)
