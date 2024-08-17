package ru.slartus.boostbuddy.ui.screens.post

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.seiko.imageloader.rememberImagePainter
import kotlinx.collections.immutable.ImmutableList
import ru.slartus.boostbuddy.components.post.PostComponent
import ru.slartus.boostbuddy.components.post.PostViewItem
import ru.slartus.boostbuddy.components.post.PostViewState
import ru.slartus.boostbuddy.data.repositories.comments.models.Comment
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.Poll
import ru.slartus.boostbuddy.data.repositories.models.PollOption
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.ui.common.HorizontalSpacer
import ru.slartus.boostbuddy.ui.common.VerticalSpacer
import ru.slartus.boostbuddy.ui.screens.blog.ContentView
import ru.slartus.boostbuddy.ui.screens.blog.FocusableBox
import ru.slartus.boostbuddy.ui.screens.blog.PostView
import ru.slartus.boostbuddy.ui.widgets.ErrorView
import ru.slartus.boostbuddy.ui.widgets.LoaderView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PostScreen(component: PostComponent) {
    val state by component.viewStates.subscribeAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.post.title) },
                navigationIcon = {
                    IconButton(onClick = {
                        component.onBackClicked()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val progressState = state.progressProgressState) {
                is PostViewState.ProgressState.Error -> ErrorView(
                    message = progressState.description,
                    onRepeatClick = {
                        component.onRepeatClicked()
                    }
                )

                PostViewState.ProgressState.Init,
                PostViewState.ProgressState.Loading -> LoaderView()

                is PostViewState.ProgressState.Loaded ->
                    if (state.items.isEmpty())
                        EmptyCommentsView()
                    else
                        FullPostView(
                            post = state.post,
                            items = state.items,
                            onMoreClick = {
                                component.onMoreCommentsClicked()
                            },
                            onMoreRepliesClick = {
                                component.onMoreRepliesClicked(it)
                            },
                            onVideoClick = {
                                component.onVideoItemClicked(state.post.id, it)
                            },
                            onPollOptionClick = { poll, option ->
                                component.onPollOptionClicked(poll, option)
                            },
                            onVoteClick = { component.onVoteClicked(it) },
                            onDeleteVoteClick = { component.onDeleteVoteClicked(it) }
                        )
            }
        }
    }
}

@Composable
private fun FullPostView(
    post: Post,
    items: ImmutableList<PostViewItem>,
    onMoreClick: () -> Unit,
    onMoreRepliesClick: (PostViewItem.CommentItem) -> Unit,
    onVideoClick: (okVideoData: Content.OkVideo) -> Unit,
    onPollOptionClick: (Poll, PollOption) -> Unit,
    onVoteClick: (poll: Poll) -> Unit,
    onDeleteVoteClick: (poll: Poll) -> Unit
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = 1)
    LazyColumn(modifier = Modifier.fillMaxSize(), state = state) {
        item(contentType = "post", key = "post_${post.id}") {
            PostView(
                post = post,
                onVideoClick = onVideoClick,
                onCommentsClick = {},
                onPollOptionClick = onPollOptionClick,
                onVoteClick = onVoteClick,
                onDeleteVoteClick = onDeleteVoteClick
            )
        }
        commentsView(items, onMoreClick, onMoreRepliesClick)
    }
}

@Composable
private fun EmptyCommentsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Список комментариев пуст",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun LazyListScope.commentsView(
    items: ImmutableList<PostViewItem>,
    onMoreClick: () -> Unit,
    onMoreRepliesClick: (PostViewItem.CommentItem) -> Unit
) {
    items(
        items = items,
        key = { it.id },
        contentType = {
            when (it) {
                is PostViewItem.CommentItem -> "CommentItem"
                PostViewItem.ErrorMore -> "ErrorMore"
                PostViewItem.LoadMore -> "LoadMore"
                PostViewItem.LoadingMore -> "LoadingMore"
            }
        }
    ) { commentItem ->
        when (commentItem) {
            is PostViewItem.CommentItem -> {
                CommentView(
                    commentItem.comment,
                    onMoreRepliesClick = { onMoreRepliesClick(commentItem) })
                VerticalSpacer(8.dp)
            }

            PostViewItem.ErrorMore -> FocusableBox(Modifier.fillMaxWidth()) {
                Box(Modifier.clickable { onMoreClick() }.padding(8.dp)) {
                    Text(
                        text = "Ошибка загрузки. Нажми для повтора",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            PostViewItem.LoadMore -> FocusableBox(Modifier.fillMaxWidth()) {
                Box(Modifier.clickable { onMoreClick() }.padding(8.dp)) {
                    Text(
                        text = "Показать ещё комментарии",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            PostViewItem.LoadingMore -> FocusableBox(Modifier.fillMaxWidth()) {
                Box(Modifier.padding(8.dp)) {
                    Text(
                        text = "Загрузка",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentView(
    comment: Comment,
    isReply: Boolean = false,
    onMoreRepliesClick: () -> Unit
) {
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.onPrimary)
            .padding(8.dp)
    ) {
        if (!comment.author.avatarUrl.isNullOrEmpty()) {
            Image(
                modifier = Modifier
                    .size(if (isReply) 36.dp else 48.dp)
                    .clip(CircleShape),
                painter = rememberImagePainter(comment.author.avatarUrl),
                contentDescription = "avatar",
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(
                modifier = Modifier
                    .size(if (isReply) 36.dp else 48.dp)
                    .clip(CircleShape),
                imageVector = Icons.Default.Person,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = "empty avatar"
            )
        }
        HorizontalSpacer(8.dp)
        Column {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = comment.author.name,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )
            VerticalSpacer(4.dp)
            comment.content.forEach { postData ->
                ContentView("", postData, {})
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = comment.createdAtText,
                style = MaterialTheme.typography.bodySmall
            )

            if (comment.replies.hasMore) {
                FocusableBox(Modifier.fillMaxWidth()) {
                    Box(Modifier.clickable { onMoreRepliesClick() }.padding(8.dp)) {
                        Text(
                            text = "ещё ответы",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            comment.replies.comments.forEach { reply ->
                CommentView(reply, isReply = true, onMoreRepliesClick = {})
            }
        }
    }
}