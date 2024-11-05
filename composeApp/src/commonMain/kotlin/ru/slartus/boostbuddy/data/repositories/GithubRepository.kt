package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.readRemaining
import kotlinx.io.Buffer
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
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
                    .get("repos/slartus/BoostBuddy/releases/latest")
                    .body()
            ReleaseInfo(
                version = response.name ?: return@fetchOrError null,
                info = response.body,
                androidDownloadUrl = response.assets?.firstOrNull { it.browserDownloadUrl != null }?.browserDownloadUrl
            )
        }

    suspend fun downloadFile(url: String): Result<Path> = fetchOrError {
        val tempFilePath = Path(SystemTemporaryDirectory, url.substringAfterLast('/'))
        val file = SystemFileSystem.sink(tempFilePath, append = false)
        val byteBufferSize = 1024 * 100
        val buffer = Buffer()
        httpClient.prepareGet(url).execute { httpResponse ->
            val channel: ByteReadChannel = httpResponse.body()
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(byteBufferSize.toLong())
                while (!packet.isEmpty) {
                    val bytes = packet.readBytes()
                    buffer.write(bytes, 0, bytes.size)
                    file.write(buffer, bytes.size.toLong())
                    file.flush()
                    buffer.clear()
                }
            }
        }
        file.close()
        tempFilePath
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
