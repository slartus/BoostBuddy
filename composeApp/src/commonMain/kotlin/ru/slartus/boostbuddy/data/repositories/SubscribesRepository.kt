package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.call.body
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.utils.fetchOrError

internal class SubscribesRepository(
    private val boostyApi: BoostyApi
) {
    suspend fun getSubscribes(): Result<List<SubscribeItem>> =
        fetchOrError {
            val response: SubscribesResponse = boostyApi.subscribes(
                limit = 30,
                withFollow = true
            ).body()

            response.data?.mapNotNull { it.toSubscribeItemOrNull() } ?: emptyList()
        }

}

private fun SubscribesResponse.Data.toSubscribeItemOrNull(): SubscribeItem? {
    val responseBlog = blog ?: return null
    val blog = Blog(
        title = responseBlog.title ?: return null,
        blogUrl = responseBlog.blogUrl ?: return null,
        owner = responseBlog.owner?.let {
            Owner(
                name = it.name ?: return null,
                avatarUrl = it.avatarUrl
            )
        } ?: return null,
    )
    return SubscribeItem(blog)
}

data class SubscribeItem(
    val blog: Blog
)

@Serializable
data class Blog(
    val title: String,
    val blogUrl: String,
    val owner: Owner,
)

@Serializable
data class Owner(
    val name: String,
    val avatarUrl: String?,
)


@Serializable
private data class SubscribesResponse(
    val offset: Int? = null,
    val limit: Int? = null,
    val data: List<Data>? = null,
    val total: Int? = null
) {

    @Serializable
    data class Flags(
        val hasTargets: Boolean? = null,
        val hasSubscriptionLevels: Boolean? = null,
        val showPostDonations: Boolean? = null,
        val acceptDonationMessages: Boolean? = null,
        val allowGoogleIndex: Boolean? = null,
        val allowIndex: Boolean? = null,
        val isRssFeedEnabled: Boolean? = null
    )

    @Serializable
    data class Owner(
        val name: String? = null,
        val hasAvatar: Boolean? = null,
        val avatarUrl: String? = null,
        val id: Int? = null
    )

    @Serializable
    data class Blog(
        val flags: Flags? = null,
        val title: String? = null,
        val owner: Owner? = null,
        val blogUrl: String? = null,
        val coverUrl: String? = null,
        val hasAdultContent: Boolean? = null
    )

    @Serializable
    data class Data(
        val isArchived: Boolean? = null,
        val price: Int? = null,
        val customPrice: Int? = null,
        val nextPayTime: Int? = null,
        val ownerId: Int? = null,
        val name: String? = null,
        val id: Int? = null,
        val offTime: String? = null,
        val period: Int? = null,
        val blog: Blog? = null,
        val levelId: Int? = null,
        val onTime: Int? = null
    )
}
