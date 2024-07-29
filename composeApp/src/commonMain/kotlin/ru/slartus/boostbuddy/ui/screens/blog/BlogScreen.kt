package ru.slartus.boostbuddy.ui.screens.blog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import kotlinx.collections.immutable.ImmutableList
import ru.slartus.boostbuddy.components.blog.BlogComponent
import ru.slartus.boostbuddy.components.blog.BlogItem
import ru.slartus.boostbuddy.components.blog.BlogViewState
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.Poll
import ru.slartus.boostbuddy.data.repositories.models.PollOption
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.ui.common.HorizontalSpacer
import ru.slartus.boostbuddy.ui.common.VerticalSpacer
import ru.slartus.boostbuddy.ui.common.isEndOfListReached
import ru.slartus.boostbuddy.ui.widgets.ErrorView
import ru.slartus.boostbuddy.ui.widgets.LoaderView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BlogScreen(component: BlogComponent) {
    val state by component.viewStates.subscribeAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.blog.title) },
                navigationIcon = {
                    IconButton(onClick = {
                        component.onBackClicked()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { component.onRepeatClicked() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Обновить"
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
                is BlogViewState.ProgressState.Error -> ErrorView(
                    message = progressState.description,
                    onRepeatClick = { component.onRepeatClicked() }
                )

                BlogViewState.ProgressState.Init,
                BlogViewState.ProgressState.Loading -> LoaderView()

                is BlogViewState.ProgressState.Loaded ->
                    PostsView(
                        items = state.items,
                        canLoadMore = state.hasMore,
                        onVideoItemClick = { post, data ->
                            component.onVideoItemClicked(
                                post.post.id,
                                data
                            )
                        },
                        onScrolledToEnd = { component.onScrolledToEnd() },
                        onErrorItemClick = { component.onErrorItemClicked() },
                        onCommentsClick = { component.onCommentsClicked(it) },
                        onPollOptionClick = { poll, option ->
                            component.onPollOptionClicked(
                                poll,
                                option
                            )
                        },
                        onVoteClick = { component.onVoteClicked(it) },
                        onDeleteVoteClick = { component.onDeleteVoteClicked(it) }
                    )
            }
        }
    }
    val dialogSlot by component.dialogSlot.subscribeAsState()
    dialogSlot.child?.instance?.also { videoTypeComponent ->
        VideoTypeDialogView(
            modifier = Modifier,
            component = videoTypeComponent
        )
    }
}

@Composable
private fun PostsView(
    items: ImmutableList<BlogItem>,
    canLoadMore: Boolean,
    onVideoItemClick: (BlogItem.PostItem, Content.OkVideo) -> Unit,
    onScrolledToEnd: () -> Unit,
    onErrorItemClick: () -> Unit,
    onCommentsClick: (post: Post) -> Unit,
    onPollOptionClick: (Poll, PollOption) -> Unit,
    onVoteClick: (poll: Poll) -> Unit,
    onDeleteVoteClick: (poll: Poll) -> Unit
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
                is BlogItem.ErrorItem -> ErrorView(
                    item.description,
                    onClick = { onErrorItemClick() })

                BlogItem.LoadingItem -> LoadingView()
                is BlogItem.PostItem -> PostView(
                    item.post,
                    onVideoClick = { onVideoItemClick(item, it) },
                    onCommentsClick = { onCommentsClick(item.post) },
                    onPollOptionClick = onPollOptionClick,
                    onVoteClick = onVoteClick,
                    onDeleteVoteClick = onDeleteVoteClick
                )
            }
        }
    }
    LaunchedEffect(endOfListReached.value, items) {
        if (endOfListReached.value && canLoadMore && items.last() is BlogItem.PostItem) {
            onScrolledToEnd()
        }
    }
}

@Composable
internal fun PostView(
    post: Post,
    onVideoClick: (okVideoData: Content.OkVideo) -> Unit,
    onCommentsClick: () -> Unit,
    onPollOptionClick: (Poll, PollOption) -> Unit,
    onVoteClick: (poll: Poll) -> Unit,
    onDeleteVoteClick: (poll: Poll) -> Unit
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.onPrimary)
            .padding(16.dp),
        horizontalAlignment = CenterHorizontally
    ) {
        FocusableBox {
            Text(
                modifier = Modifier.fillMaxWidth().focusable(),
                text = post.title,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        }
        VerticalSpacer(16.dp)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            post.data.forEach { postData ->
                ContentView(post.signedQuery, postData, onVideoClick)
            }
        }
        if (post.poll != null) {
            VerticalSpacer(16.dp)
            PollView(
                poll = post.poll,
                onOptionClick = onPollOptionClick,
                onVoteClick = { onVoteClick(post.poll) },
                onDeleteVoteClick = { onDeleteVoteClick(post.poll) }
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