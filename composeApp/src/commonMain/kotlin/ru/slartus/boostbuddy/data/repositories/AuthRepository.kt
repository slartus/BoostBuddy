package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import kotlinx.serialization.Serializable

class AuthRepository(
    private val httpClient: HttpClient
) {
    suspend fun refreshToken(accessToken: String, refreshToken: String): Auth {
        val response = httpClient.post {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody(FormDataContent(Parameters.build {
                append("device_id", "560424b1-9a82-40f8-a7bd-88275ce27b4b")
                append("device_os", "android")
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
            }))
        }.body<AuthResponse>()
        return Auth(
            accessToken = response.accessToken!!,
            refreshToken = response.refreshToken!!,
            expiresAt = response.expiresAt!!
        )
    }
}

data class Auth(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long
)

@Serializable
data class AuthResponse(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresAt: Long? = null
)