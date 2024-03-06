package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

class SubscribesRepository(
    private val httpClient: HttpClient
) {
//    suspend fun getSubscribes(accessToken: String): Map<Int, String> {
//        val response =
//            httpClient.get("https://api.boosty.to/v1/user/subscriptions?limit=30&with_follow=true") {
//                header(HttpHeaders.Authorization, "Bearer $accessToken")
//            }.body<SubscribesResponse>()
//        return response?.data?.map { it.id!! to it.name!! }?: emptyMap()
//    }
}

@Serializable
private data class SubscribesResponse(
    val offset: Int? = null,
    val limit: Int? = null,
    val data: List<Data>? = null,
    val total: Int? = null
)

@Serializable
private data class Flags(
    val hasTargets: Boolean? = null,
    val hasSubscriptionLevels: Boolean? = null,
    val showPostDonations: Boolean? = null,
    val acceptDonationMessages: Boolean? = null,
    val allowGoogleIndex: Boolean? = null,
    val allowIndex: Boolean? = null,
    val isRssFeedEnabled: Boolean? = null
)

@Serializable
private data class Owner(
    val name: String? = null,
    val hasAvatar: Boolean? = null,
    val avatarUrl: String? = null,
    val id: Int? = null
)

@Serializable
private data class Blog(
    val flags: Flags? = null,
    val title: String? = null,
    val owner: Owner? = null,
    val blogUrl: String? = null,
    val coverUrl: String? = null,
    val hasAdultContent: Boolean? = null
)

@Serializable
private data class Data(
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