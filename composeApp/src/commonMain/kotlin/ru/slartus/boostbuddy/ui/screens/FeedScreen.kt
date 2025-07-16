package ru.slartus.boostbuddy.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.slartus.boostbuddy.components.common.ProgressState
import ru.slartus.boostbuddy.components.feed.FeedComponent
import ru.slartus.boostbuddy.ui.widgets.ErrorView
import ru.slartus.boostbuddy.ui.widgets.LoaderView

@Composable
internal fun FeedScreen(
    component: FeedComponent,
    modifier: Modifier = Modifier,
) {
    val state by component.viewStates.subscribeAsState()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val progressState = state.progressState) {
            is ProgressState.Error -> ErrorView(
                message = progressState.description,
                onRepeatClick = { component.onRepeatClicked() }
            )

            ProgressState.Init,
            ProgressState.Loading -> LoaderView()

            is ProgressState.Loaded ->
                PostsView(
                    items = state.items,
                    showBlogInfo = true,
                    canLoadMore = state.hasMore,
                    onVideoItemClick = { post, data ->
                        component.onVideoItemClicked(
                            post.post,
                            data
                        )
                    },
                    onScrolledToEnd = { component.onScrolledToEnd() },
                    onErrorItemClick = { component.onErrorItemClicked() },
                    onCommentsClick = { component.onCommentsClicked(it) },
                    onPollOptionClick = { post, poll, option ->
                        component.onPollOptionClicked(post, poll, option)
                    },
                    onVoteClick = { post, poll -> component.onVoteClicked(post, poll) },
                    onDeleteVoteClick = { post, poll ->
                        component.onDeleteVoteClicked(
                            post,
                            poll
                        )
                    },
                    onBlogClick = { post -> component.onBlogClicked(post) }
                )
        }
    }
}
