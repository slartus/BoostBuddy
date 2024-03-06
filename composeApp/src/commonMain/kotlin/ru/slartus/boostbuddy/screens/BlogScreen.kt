package ru.slartus.boostbuddy.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.seiko.imageloader.rememberImagePainter
import ru.slartus.boostbuddy.components.BlogComponent
import ru.slartus.boostbuddy.components.BlogViewState
import ru.slartus.boostbuddy.components.SubscribesViewState
import ru.slartus.boostbuddy.data.repositories.Blog
import ru.slartus.boostbuddy.data.repositories.Post

@Composable
fun BlogScreen(component: BlogComponent) {
    val state = component.state.subscribeAsState().value

    Column {
        Text(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            text = state.blog.title
        )
        when (val progressState = state.progressProgressState) {
            is BlogViewState.ProgressState.Error -> Text(text = progressState.description)
            BlogViewState.ProgressState.Init,
            BlogViewState.ProgressState.Loading -> Text(text = "Загрузка")

            is BlogViewState.ProgressState.Loaded -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(progressState.items) { item ->
                        PostView(item, onClick = { component.onItemClicked(item) })
                    }
                }
            }
        }
    }
}


@Composable
private fun PostView(post: Post, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable { onClick() }.padding(16.dp)) {
        Row {
            if (post.user.avatarUrl != null) {
                Image(
                    modifier = Modifier.size(48.dp),
                    painter = rememberImagePainter(post.user.avatarUrl),
                    contentDescription = "image",
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = post.user.name
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = post.title
        )
        if (post.previewUrl != null) {
            Image(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                painter = rememberImagePainter(post.previewUrl),
                contentDescription = "image",
            )
        }
    }
}