package ru.slartus.boostbuddy.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
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
            SubscribesViewState.ProgressState.Error -> Text(text = "Ошибка")
            SubscribesViewState.ProgressState.Init,
            SubscribesViewState.ProgressState.Loading -> Text(text = "Загрузка")
            is SubscribesViewState.ProgressState.Loaded -> {
                Text(text = "Загружено ${progressState.items.size}")
                LazyColumn(modifier = Modifier.fillMaxSize()) {
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
    Text(
        modifier = Modifier.fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        text = blog.title
    )
}