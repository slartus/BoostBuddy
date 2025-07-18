package ru.slartus.boostbuddy.ui.screens.filter

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import ru.slartus.boostbuddy.components.filter.tags.FilterTagsComponent
import ru.slartus.boostbuddy.components.filter.tags.TagItem
import ru.slartus.boostbuddy.ui.common.BottomView
import ru.slartus.boostbuddy.ui.common.CheckboxBottomViewItem
import ru.slartus.boostbuddy.ui.common.LoadingViewItem
import ru.slartus.boostbuddy.ui.common.TextBottomViewItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterTagsDialogView(
    component: FilterTagsComponent,
    onDismissClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissClicked,
        sheetState = sheetState
    ) {
        FilterTagsContent(component)
    }
}

@Composable
private fun FilterTagsContent(
    component: FilterTagsComponent,
    modifier: Modifier = Modifier
) {
    val state by component.viewStates.subscribeAsState()
    val listState = rememberLazyListState()

    HandleListScrollEnd(listState) {
        if (state.loadMore) {
            component.loadNextPage()
        }
    }

    BottomView(
        modifier = modifier,
        title = "Теги",
    ) {
        TagsList(
            tags = state.tags,
            listState = listState,
            onTagSelect = component::onTagSelect,
            onRetryClick = component::onRepeatClick
        )
    }
}

@Composable
private fun HandleListScrollEnd(
    listState: androidx.compose.foundation.lazy.LazyListState,
    onReachEnd: () -> Unit
) {
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .map { layoutInfo ->
                layoutInfo.visibleItemsInfo.lastOrNull()?.let { lastItem ->
                    lastItem.index == layoutInfo.totalItemsCount - 1
                } ?: false
            }
            .distinctUntilChanged()
            .collect { reachedEnd ->
                if (reachedEnd) onReachEnd()
            }
    }
}

@Composable
private fun TagsList(
    tags: List<TagItem>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onTagSelect: (TagItem.TagModel) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
    ) {
        items(
            items = tags,
            key = { it.key },
            contentType = { it.contentType }
        ) { tagItem ->
            when (tagItem) {
                TagItem.Loading -> LoadingViewItem()
                is TagItem.TagModel -> TagItemView(tagItem, onTagSelect)
                TagItem.Error -> RetryView(onRetryClick)
            }
        }
    }
}

@Composable
private fun TagItemView(
    tagItem: TagItem.TagModel,
    onTagSelect: (TagItem.TagModel) -> Unit
) {
    CheckboxBottomViewItem(
        text = tagItem.tag.title,
        checked = tagItem.selected,
        onCheckedChange = { onTagSelect(tagItem) }
    )
}

@Composable
private fun RetryView(
    onRetryClick: () -> Unit
) {
    TextBottomViewItem(
        text = "Повторить загрузку",
        onClick = onRetryClick
    )
}

private val TagItem.key: String
    get() = when (this) {
        TagItem.Error -> "error_item"
        TagItem.Loading -> "loading_item"
        is TagItem.TagModel -> "tag_${this.tag.title}"
    }

private val TagItem.contentType: String
    get() = when (this) {
        TagItem.Error -> "error"
        TagItem.Loading -> "loading"
        is TagItem.TagModel -> "tag"
    }