package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.call.body
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.data.api.BoostyApi
import ru.slartus.boostbuddy.utils.fetchOrError

internal class ProfileRepository(
    private val boostyApi: BoostyApi
) {
    suspend fun getProfile(): Result<Profile> =
        fetchOrError {
            val response: ProfileResponse = boostyApi.current().body()

            Profile(
                id = response.id ?: error("wrong data"),
                blogUrl = response.blogUrl,
                name = response.name,
                avatarUrl = response.avatarUrl,
            )
        }
}

data class Profile(
    val id: String,
    val blogUrl: String?,
    val name: String?,
    val avatarUrl: String?,
)

@Serializable
private data class ProfileResponse(
    val id: String? = null,
    val blogUrl: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null,
)