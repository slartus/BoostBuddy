package ru.slartus.boostbuddy.ui.screens.blog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.slartus.boostbuddy.components.blog.BlogComponent
import ru.slartus.boostbuddy.components.common.ProgressState
import ru.slartus.boostbuddy.ui.screens.PostsView
import ru.slartus.boostbuddy.ui.screens.filter.FilterDialogView
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
                    IconButton(onClick = component::onFilterClick) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = "Фильтр"
                        )
                    }
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
                        showBlogInfo = false,
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
                        onBlogClick = {}
                    )
            }
        }
    }

    val dialogSlot by component.dialogSlot.subscribeAsState()
    dialogSlot.child?.instance?.let { dialogComponent ->
        when (dialogComponent) {
            is BlogComponent.DialogChild.Filter -> FilterDialogView(
                component = dialogComponent.component,
                onDismissClicked = component::onDialogDismissed,
            )
        }
    }
}
