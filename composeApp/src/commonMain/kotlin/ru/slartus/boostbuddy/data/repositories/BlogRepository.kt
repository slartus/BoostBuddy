package ru.slartus.boostbuddy.data.repositories

import ru.slartus.boostbuddy.components.filter.AccessType
import ru.slartus.boostbuddy.components.filter.Filter
import ru.slartus.boostbuddy.data.api.BoostyApi
import ru.slartus.boostbuddy.data.api.model.RemoteBlogInfoResponse
import ru.slartus.boostbuddy.data.api.model.RemotePostResponse
import ru.slartus.boostbuddy.data.repositories.models.Extra
import ru.slartus.boostbuddy.data.repositories.models.Poll
import ru.slartus.boostbuddy.data.repositories.models.PollOption
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.data.repositories.models.PostCount
import ru.slartus.boostbuddy.data.repositories.models.Posts
import ru.slartus.boostbuddy.data.repositories.models.mapToContentOrNull
import ru.slartus.boostbuddy.data.repositories.models.mapToUserOrNull
import ru.slartus.boostbuddy.data.repositories.models.mergeText
import ru.slartus.boostbuddy.utils.fetchOrError

internal class BlogRepository(
    private val boostyApi: BoostyApi,
) {
    suspend fun fetchPosts(
        url: String,
        filter: Filter,
        limit: Int = 20,
        offset: String? = null,
        commentsLimit: Int = 0,
        replyLimit: Int = 0,
    ): Result<Posts> =
        fetchOrError {
            val response = boostyApi.blogPosts(
                blog = url,
                limit = limit,
                offset = offset,
                commentsLimit = commentsLimit,
                replyLimit = replyLimit,
                isOnlyAllowed = filter.accessType == AccessType.Allowed,
                fromDate = filter.period?.from,
                toDate = filter.period?.to,
                tagsIds = filter.tags.map { it.id },
                onlyBought = filter.accessType == AccessType.Bought,
            )

            Posts(
                items = response.data?.mapNotNull { it.mapToPostOrNull() } ?: emptyList(),
                extra = response.extra?.mapToExtraOrNull(),
            )
        }

    suspend fun searchPosts(
        url: String,
        filter: Filter,
        query: String,
        limit: Int = 20,
        offset: String? = null,
        commentsLimit: Int = 0,
        replyLimit: Int = 0,
    ): Result<Posts> =
        fetchOrError {
            val response = boostyApi.blogSearchPosts(
                blog = url,
                limit = limit,
                offset = offset,
                commentsLimit = commentsLimit,
                replyLimit = replyLimit,
                isOnlyAllowed = filter.accessType == AccessType.Allowed,
                fromDate = filter.period?.from,
                toDate = filter.period?.to,
                tagsIds = filter.tags.map { it.id },
                onlyBought = filter.accessType == AccessType.Bought,
                query = query,
            )

            Posts(
                items = response.data?.searchPosts?.mapNotNull { it.post?.mapToPostOrNull() }
                    ?: emptyList(),
                extra = response.extra?.mapToExtraOrNull(),
            )
        }

    suspend fun fetchInfo(blogUrl: String): Result<Blog> =
        fetchOrError {
            val response: RemoteBlogInfoResponse = boostyApi.blogInfo(blogUrl)

            Blog(
                title = response.title.orEmpty(),
                blogUrl = response.blogUrl ?: error("Blog blogUrl is null"),
                owner = response.owner?.let {
                    Owner(
                        name = it.name.orEmpty(),
                        avatarUrl = it.avatarUrl
                    )
                } ?: error("Blog owner is null"),
            )
        }
}

internal fun RemotePostResponse.Extra.mapToExtraOrNull(): Extra? {
    return Extra(
        offset = offset ?: return null,
        isLast = isLast ?: true
    )
}

internal fun RemotePostResponse.Post.mapToPostOrNull(): Post? {
    return Post(
        id = id ?: return null,
        createdAt = createdAt ?: return null,
        signedQuery = signedQuery.orEmpty(),
        intId = intId ?: return null,
        title = title.orEmpty(),
        data = data?.mapNotNull { it.mapToContentOrNull() }.orEmpty().mergeText(),
        teaser = teaser?.mapNotNull { it.mapToContentOrNull() }.orEmpty().mergeText(),
        user = user?.mapToUserOrNull() ?: return null,
        count = PostCount(likes = count?.likes ?: 0, comments = count?.comments ?: 0),
        poll = poll?.mapToPostPollOrNull(),
        hasAccess = hasAccess ?: true
    )
}

internal fun RemotePostResponse.Poll.mapToPostPollOrNull(): Poll? {
    return Poll(
        id = id ?: return null,
        title = title.orEmpty(),
        isMultiple = isMultiple ?: return null,
        isFinished = isFinished ?: return null,
        options = options?.mapNotNull { it.mapToPostPollOptionOrNull() }?.ifEmpty { null }
            ?: return null,
        counter = counter ?: 0,
        answer = answer.orEmpty()
    )
}

private fun RemotePostResponse.PollOption.mapToPostPollOptionOrNull(): PollOption? {
    return PollOption(
        id = id ?: return null,
        text = text.orEmpty(),
        counter = counter ?: return null,
        fraction = fraction ?: return null
    )
}