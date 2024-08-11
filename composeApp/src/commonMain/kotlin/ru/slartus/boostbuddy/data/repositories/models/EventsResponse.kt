package ru.slartus.boostbuddy.data.repositories.models

import kotlinx.serialization.Serializable

@Serializable
internal data class EventsResponse(
    val data: Data? = null
) {
    @Serializable
    data class Data(
        val notificationStandalone: NotificationStandalone? = null
    )

    @Serializable
    data class NotificationStandalone(
        val updateTime: Int? = null,
        val count: Count? = null,
        val events: List<Event>? = null,
    )

    @Serializable
    data class Event(
        val isRead: Boolean? = null,
        val blog: Blog? = null,
        val type: String? = null,
        val eventTime: Int? = null,
        val id: Int? = null,
        val aggregation: Aggregation? = null,
        val author: Author? = null,
    )

    @Serializable
    data class Author(
        val avatarUrl: String? = null,
        val hasAvatar: Boolean? = null,
        val name: String? = null,
        val id: Int? = null
    )

    @Serializable
    data class Aggregation(
        val type: String? = null,
        val id: Int? = null
    )

    @Serializable
    data class Blog(
        val hasAdultContent: Boolean? = null,
        val title: String? = null,
        val flags: Flags? = null,
        val owner: Owner? = null,
        val coverUrl: String? = null,
        val blogUrl: String? = null
    )

    @Serializable
    data class Owner(
        val name: String? = null,
        val id: Int? = null,
        val hasAvatar: Boolean? = null,
        val avatarUrl: String? = null
    )

    @Serializable
    data class Flags(
        val isRssFeedEnabled: Boolean? = null,
        val allowIndex: Boolean? = null,
        val showPostDonations: Boolean? = null,
        val acceptDonationMessages: Boolean? = null,
        val hasSubscriptionLevels: Boolean? = null,
        val allowGoogleIndex: Boolean? = null,
        val hasTargets: Boolean? = null
    )

    @Serializable
    data class Count(
        val total: Int? = null,
        val unread: Int? = null,
        val byEventType: List<ByEventType>? = null
    )

    @Serializable
    data class ByEventType(
        /**
         * @sample 'blog_stat_report_failed,blog_stat_report_ok,donation_new,message_buy,post_buy,
         *          post_comment_new,post_new,promo_new,referal_new_blogger,referal_new_user,
         *          subscriber_new,support_ticket_update,target_new
         */
        val type: String? = null,
        val unread: Int? = null,
        val total: Int? = null
    )
}