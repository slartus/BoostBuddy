package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.utils.fetchOrError

internal class GithubRepository(
    private val httpClient: HttpClient
) {
    suspend fun getLastReleaseInfo(): Result<ReleaseInfo?> =
        fetchOrError {
            val response: ReleaseInfoResponse =
                httpClient
                    .get("https://api.github.com/repos/slartus/BoostBuddy/releases/latest")
                    .body()
            ReleaseInfo(
                version = response.name ?: return@fetchOrError null,
                info = response.body,
                androidDownloadUrl = response.assets?.firstOrNull { it.browserDownloadUrl != null }?.browserDownloadUrl
            )
        }
}

@Serializable
data class ReleaseInfo(
    val version: String,
    val info: String?,
    val androidDownloadUrl: String?
)

@Serializable
private data class ReleaseInfoResponse(
    val name: String? = null,
    val assets: List<ReleaseAsset>? = null,
    val body: String? = null
)

@Serializable
private data class ReleaseAsset(
    @SerialName("browser_download_url")
    val browserDownloadUrl: String? = null
)
