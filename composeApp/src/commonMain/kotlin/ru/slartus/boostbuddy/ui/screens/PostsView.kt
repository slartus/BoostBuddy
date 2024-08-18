package ru.slartus.boostbuddy.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.seiko.imageloader.rememberImagePainter
import kotlinx.collections.immutable.ImmutableList
import ru.slartus.boostbuddy.components.feed.FeedPostItem
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.Poll
import ru.slartus.boostbuddy.data.repositories.models.PollOption
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.data.repositories.models.User
import ru.slartus.boostbuddy.ui.common.HorizontalSpacer
import ru.slartus.boostbuddy.ui.common.VerticalSpacer
import ru.slartus.boostbuddy.ui.common.isEndOfListReached
import ru.slartus.boostbuddy.ui.screens.blog.ContentView
import ru.slartus.boostbuddy.ui.screens.blog.FocusableBox
import ru.slartus.boostbuddy.ui.screens.blog.PollView


@Composable
internal fun PostsView(
    items: ImmutableList<FeedPostItem>,
    showBlogInfo: Boolean,
    canLoadMore: Boolean,
    onVideoItemClick: (FeedPostItem.PostItem, Content.OkVideo) -> Unit,
    onScrolledToEnd: () -> Unit,
    onErrorItemClick: () -> Unit,
    onBlogClick: (post: Post) -> Unit,
    onCommentsClick: (post: Post) -> Unit,
    onPollOptionClick: (Post, Poll, PollOption) -> Unit,
    onVoteClick: (Post, poll: Poll) -> Unit,
    onDeleteVoteClick: (Post, poll: Poll) -> Unit
) {
    val listScrollState = rememberLazyListState()

    val endOfListReached = remember {
        derivedStateOf {
            listScrollState.isEndOfListReached(visibleThreshold = 3)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listScrollState,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.key }, contentType = { it.contentType }) { item ->
            when (item) {
                is FeedPostItem.ErrorItem -> ErrorView(
                    item.description,
                    onClick = { onErrorItemClick() })

                FeedPostItem.LoadingItem -> LoadingView()
                is FeedPostItem.PostItem -> PostView(
                    post = item.post,
                    showBlogInfo = showBlogInfo,
                    onVideoClick = { onVideoItemClick(item, it) },
                    onCommentsClick = { onCommentsClick(item.post) },
                    onPollOptionClick = onPollOptionClick,
                    onVoteClick = onVoteClick,
                    onDeleteVoteClick = onDeleteVoteClick,
                    onBlogClick = { onBlogClick(item.post) }
                )
            }
        }
    }
    LaunchedEffect(endOfListReached.value, items) {
        if (endOfListReached.value && canLoadMore && items.last() is FeedPostItem.PostItem) {
            onScrolledToEnd()
        }
    }
}

@Composable
internal fun PostView(
    post: Post,
    showBlogInfo: Boolean,
    onBlogClick: () -> Unit,
    onVideoClick: (okVideoData: Content.OkVideo) -> Unit,
    onCommentsClick: () -> Unit,
    onPollOptionClick: (Post, Poll, PollOption) -> Unit,
    onVoteClick: (Post, poll: Poll) -> Unit,
    onDeleteVoteClick: (Post, poll: Poll) -> Unit
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.onPrimary)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showBlogInfo) {
            BlogInfo(post.user, onBlogClick)
        }
        if (post.title.isNotEmpty()) {
            TitleView(post.title)
            VerticalSpacer(16.dp)
        }
        if (post.hasAccess || post.teaser.isEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                post.data.forEach { postData ->
                    ContentView(post.signedQuery, postData, onVideoClick)
                }
            }
        }

        if (!post.hasAccess || post.data.isEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                post.teaser.forEach { postData ->
                    ContentView(post.signedQuery, postData, onVideoClick)
                }
            }
        }

        if (post.poll != null) {
            VerticalSpacer(16.dp)
            PollView(
                poll = post.poll,
                onOptionClick = { option -> onPollOptionClick(post, post.poll, option) },
                onVoteClick = { onVoteClick(post, post.poll) },
                onDeleteVoteClick = { onDeleteVoteClick(post, post.poll) }
            )
        }

        VerticalSpacer(8.dp)
        FocusableBox(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .clickable { onCommentsClick() }
            ) {
                CountView(
                    icon = Icons.Default.Favorite,
                    text = post.count.likes.toString()
                )
                HorizontalSpacer(16.dp)
                CountView(
                    icon = Icons.AutoMirrored.Default.Comment,
                    text = post.count.comments.toString()
                )
            }
        }
    }
}

@Composable
private fun BlogInfo(
    user: User,
    onClick: () -> Unit
) {
    FocusableBox {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).clickable {
                onClick()
            },
            verticalAlignment = CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            user.avatarUrl?.let { avatarUrl ->
                Image(
                    modifier = Modifier.size(32.dp),
                    painter = rememberImagePainter(avatarUrl),
                    contentDescription = "url",
                )
            }
            Text(
                modifier = Modifier.fillMaxWidth().focusable(),
                text = user.name,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Composable
private fun TitleView(text: String) {
    FocusableBox {
        Text(
            modifier = Modifier.fillMaxWidth().focusable(),
            text = text,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun CountView(icon: ImageVector, text: String) {
    Row {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = icon,
            contentDescription = "Icon"
        )
        HorizontalSpacer(8.dp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(Modifier.size(24.dp))
    }
}

@Composable
private fun ErrorView(description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer)
            .clickable { onClick() }.padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.titleMedium
        )
    }
}