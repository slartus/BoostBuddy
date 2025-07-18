package ru.slartus.boostbuddy.ui.screens.filter

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
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
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    ModalBottomSheet(
        modifier = Modifier,
        onDismissRequest = { onDismissClicked() },
        sheetState = sheetState
    ) {
        FilterTagsView(component)
    }
}

@Composable
private fun FilterTagsView(
    component: FilterTagsComponent,
) {
    val state by component.viewStates.subscribeAsState()
    BottomView("Теги") {
        LazyColumn {
            items(
                state.tags,
                key = { it.key },
                contentType = { it.contentType }
            ) { tagItem ->
                when (tagItem) {
                    TagItem.Loading -> LoadingViewItem()

                    is TagItem.TagModel ->
                        CheckboxBottomViewItem(
                            text = tagItem.tag.title,
                            checked = tagItem.selected,
                            onCheckedChange = {
                                component.onTagSelect(tagItem)
                            }
                        )

                    TagItem.Error ->
                        TextBottomViewItem(
                            text = "Повторить загрузку",
                            onClick = component::onRepeatClick,
                        )
                }
            }
        }
    }
}

private val TagItem.key: String
    get() = when (this) {
        TagItem.Error -> "#TagItem.Error#"
        TagItem.Loading -> "#TagItem.Loading#"
        is TagItem.TagModel -> this.tag.title
    }

private val TagItem.contentType: String
    get() = when (this) {
        TagItem.Error -> "Error"
        TagItem.Loading -> "Loading"
        is TagItem.TagModel -> "TagModel"
    }