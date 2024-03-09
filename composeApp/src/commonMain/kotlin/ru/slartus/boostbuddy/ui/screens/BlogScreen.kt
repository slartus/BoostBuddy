package ru.slartus.boostbuddy.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.seiko.imageloader.rememberImagePainter
import kotlinx.collections.immutable.ImmutableList
import ru.slartus.boostbuddy.components.blog.BlogComponent
import ru.slartus.boostbuddy.components.blog.BlogItem
import ru.slartus.boostbuddy.components.blog.BlogViewState
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.data.repositories.models.PostData
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration
import ru.slartus.boostbuddy.ui.common.isEndOfListReached
import ru.slartus.boostbuddy.ui.widgets.ErrorView
import ru.slartus.boostbuddy.ui.widgets.LoaderView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlogScreen(component: BlogComponent) {
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
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Localized description"
                        )
                    }
                },
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
                        onVideoItemClick = { component.onVideoItemClicked(it) },
                        onScrolledToEnd = { component.onScrolledToEnd() },
                        onErrorItemClick = { component.onErrorItemClicked() },
                    )
            }
        }

        val dialogSlot by component.dialogSlot.subscribeAsState()
        dialogSlot.child?.instance?.also { videoTypeComponent ->
            VideoTypeDialogView(
                postData = videoTypeComponent.postData,
                onDismissClicked = { videoTypeComponent.onDismissClicked() },
                onItemClicked = { videoTypeComponent.onItemClicked(it) }
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoTypeDialogView(
    postData: PostData.OkVideo,
    onItemClicked: (PlayerUrl) -> Unit,
    onDismissClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    ModalBottomSheet(
        modifier = modifier.navigationBarsPadding(),
        onDismissRequest = { onDismissClicked() },
        sheetState = sheetState
    ) {
        Column {
            postData.playerUrls.filter { it.url.isNotEmpty() }.forEach {
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { onItemClicked(it) }
                        .padding(16.dp),
                    text = it.type
                )
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun PostsView(
    items: ImmutableList<BlogItem>,
    canLoadMore: Boolean,
    onVideoItemClick: (PostData.OkVideo) -> Unit,
    onScrolledToEnd: () -> Unit,
    onErrorItemClick: () -> Unit
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
                    onVideoClick = { onVideoItemClick(it) })
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
private fun PostView(post: Post, onVideoClick: (okVideoData: PostData.OkVideo) -> Unit) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
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
        Spacer(modifier = Modifier.size(16.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            post.data.forEach { postData ->
                FocusableBox {
                    when (postData) {
                        is PostData.Text -> PostDataTextView(postData)
                        PostData.Unknown -> PostDataUnknownView()
                        is PostData.OkVideo -> PostDataOkVideoView(postData, onVideoClick)
                        is PostData.Image -> PostDataImageView(postData)
                        is PostData.Link -> PostDataLinkView(postData)
                        is PostData.Video -> PostDataVideoView(postData)
                        is PostData.AudioFile -> PostDataAudioFileView(postData)
                    }
                }
            }
        }
    }
}

@Composable
private fun PostDataAudioFileView(
    postData: PostData.AudioFile
) {
    val platformConfiguration = LocalPlatformConfiguration.current
    Text(
        modifier = Modifier.fillMaxWidth().clickable {
            platformConfiguration.openBrowser(postData.url)
        },
        text = postData.title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium
    )
}

@Composable
private fun PostDataVideoView(
    postData: PostData.Video
) {
    val platformConfiguration = LocalPlatformConfiguration.current
    Text(
        modifier = Modifier.fillMaxWidth().clickable {
            platformConfiguration.openBrowser(postData.url)
        },
        text = postData.url,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium
    )
}


@Composable
private fun PostDataLinkView(
    postData: PostData.Link
) {
    val platformConfiguration = LocalPlatformConfiguration.current

    Text(
        modifier = Modifier.fillMaxWidth().clickable {
            platformConfiguration.openBrowser(postData.url)
        },
        text = postData.text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium
    )
}

@Composable
private fun PostDataImageView(postData: PostData.Image) {
    Box(modifier = Modifier.heightIn(min = 200.dp).focusable()) {
        Image(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
                .wrapContentHeight(),
            painter = rememberImagePainter(postData.url),
            contentDescription = "url",
        )
    }
}

@Composable
private fun PostDataOkVideoView(
    postData: PostData.OkVideo,
    onVideoClick: (okVideoData: PostData.OkVideo) -> Unit,
) {
    Box(
        modifier = Modifier
            .clickable { onVideoClick(postData) }
            .heightIn(min = 200.dp)
    ) {
        Image(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()
                .wrapContentHeight(),
            painter = rememberImagePainter(postData.previewUrl),
            contentDescription = "preview",
        )
    }
}

@Composable
private fun FocusableBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier
        .then(
            if (focused) Modifier.border(0.5.dp, MaterialTheme.colorScheme.primary)
            else Modifier
        )
        .onFocusChanged { focused = it.isFocused }
        .focusable()) {
        content()
    }
}

@Composable
private fun PostDataUnknownView() {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = "UNKNOWN_CONTENT",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun PostDataTextView(postData: PostData.Text) {
    Text(
        modifier = Modifier.fillMaxWidth().focusable(),
        text = postData.content.text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium
    )
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