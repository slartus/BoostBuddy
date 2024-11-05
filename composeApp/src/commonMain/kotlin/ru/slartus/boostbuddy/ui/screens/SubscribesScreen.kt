package ru.slartus.boostbuddy.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.seiko.imageloader.rememberImagePainter
import kotlinx.collections.immutable.ImmutableList
import ru.slartus.boostbuddy.components.subscribes.SubscribesComponent
import ru.slartus.boostbuddy.components.subscribes.SubscribesViewState
import ru.slartus.boostbuddy.data.repositories.Blog
import ru.slartus.boostbuddy.data.repositories.SubscribeItem
import ru.slartus.boostbuddy.ui.widgets.EmptyView
import ru.slartus.boostbuddy.ui.widgets.ErrorView
import ru.slartus.boostbuddy.ui.widgets.LoaderView

@Composable
internal fun SubscribesScreen(component: SubscribesComponent) {
    val state by component.viewStates.subscribeAsState()
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Center
    ) {
        when (val progressState = state.progressProgressState) {
            is SubscribesViewState.ProgressState.Error -> ErrorView(
                message = progressState.description,
                onRepeatClick = { component.onRepeatClicked() }
            )

            SubscribesViewState.ProgressState.Init,
            SubscribesViewState.ProgressState.Loading -> LoaderView()

            is SubscribesViewState.ProgressState.Loaded -> SubscribesView(
                items = progressState.items,
                onItemClicked = component::onItemClicked
            )
        }
    }
}


@Composable
private fun SubscribesView(
    items: ImmutableList<SubscribeItem>,
    onItemClicked: (SubscribeItem) -> Unit
) {
    if (items.isEmpty()) {
        EmptyView()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(items) { item ->
                BlogView(item.blog, onClick = { onItemClicked(item) })
            }
        }
    }
}

@Composable
private fun BlogView(blog: Blog, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.onPrimary)
            .clickable { onClick() }.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = CenterVertically
    ) {
        if (blog.owner.avatarUrl != null) {
            Image(
                modifier = Modifier.size(64.dp),
                painter = rememberImagePainter(blog.owner.avatarUrl),
                contentDescription = "avatar",
            )
            Spacer(modifier = Modifier.size(8.dp))
        }

        Column {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = blog.owner.name,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = blog.title,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}