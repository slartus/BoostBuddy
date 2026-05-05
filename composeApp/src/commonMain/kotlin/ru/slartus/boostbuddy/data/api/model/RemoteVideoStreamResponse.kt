package ru.slartus.boostbuddy.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.data.repositories.models.ContentResponse
import ru.slartus.boostbuddy.data.repositories.models.UserResponse

@Serializable
internal data class RemoteVideoStreamResponse(
    val id: String? = null,
    @SerialName("int_id") val intId: Long? = null,
    val title: String? = null,
    val isOnline: Boolean? = null,
    val isTemplate: Boolean? = null,
    val hasAccess: Boolean? = null,
    val startTime: Long? = null,
    val publishTime: Long? = null,
    val createdAt: Long? = null,
    val signedQuery: String? = null,
    val isLiked: Boolean? = null,
    val data: List<ContentResponse>? = null,
    val teaser: List<ContentResponse>? = null,
    val user: UserResponse? = null,
    val count: Count? = null,
    val subscriptionLevel: SubscriptionLevel? = null,
) {
    @Serializable
    data class Count(
        val viewers: Int? = null,
        val likes: Int? = null,
    )

    @Serializable
    data class SubscriptionLevel(
        val id: Long? = null,
        val name: String? = null,
        val currencyPrices: CurrencyPrices? = null,
    )

    @Serializable
    data class CurrencyPrices(
        @SerialName("RUB") val rub: Double? = null,
        @SerialName("USD") val usd: Double? = null,
        @SerialName("EUR") val eur: Double? = null,
    )
}
