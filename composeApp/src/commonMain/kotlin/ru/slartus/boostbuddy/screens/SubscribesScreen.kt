package ru.slartus.boostbuddy.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.seiko.imageloader.rememberImagePainter
import ru.slartus.boostbuddy.components.SubscribesComponent
import ru.slartus.boostbuddy.components.SubscribesViewState
import ru.slartus.boostbuddy.data.repositories.Blog

@Composable
fun SubscribesScreen(component: SubscribesComponent) {
    val state = component.state.subscribeAsState().value

    Column {
        Text(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            text = "Подписки"
        )
        when (val progressState = state.progressProgressState) {
            is SubscribesViewState.ProgressState.Error -> Text(text = progressState.description)
            SubscribesViewState.ProgressState.Init,
            SubscribesViewState.ProgressState.Loading -> Text(text = "Загрузка")

            is SubscribesViewState.ProgressState.Loaded -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(progressState.items) { item ->
                        BlogView(item.blog, onClick = { component.onItemClicked(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BlogView(blog: Blog, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = CenterVertically
    ) {
        if (blog.owner.avatarUrl != null) {
            Image(
                modifier = Modifier.size(64.dp),
                painter = rememberImagePainter(blog.owner.avatarUrl),
                contentDescription = "image",
            )
            Spacer(modifier = Modifier.size(8.dp))
        }

        Column {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = blog.owner.name
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = blog.title
            )
        }

    }
}