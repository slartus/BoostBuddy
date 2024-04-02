package ru.slartus.boostbuddy.ui.screens.post

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.seiko.imageloader.rememberImagePainter
import kotlinx.collections.immutable.ImmutableList
import ru.slartus.boostbuddy.components.post.CommentItem
import ru.slartus.boostbuddy.components.post.PostComponent
import ru.slartus.boostbuddy.components.post.PostViewState
import ru.slartus.boostbuddy.data.repositories.comments.models.Comment
import ru.slartus.boostbuddy.ui.common.HorizontalSpacer
import ru.slartus.boostbuddy.ui.common.VerticalSpacer
import ru.slartus.boostbuddy.ui.screens.blog.ContentView
import ru.slartus.boostbuddy.ui.widgets.ErrorView
import ru.slartus.boostbuddy.ui.widgets.LoaderView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PostScreen(component: PostComponent) {
    val state by component.viewStates.subscribeAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = {
                        component.onBackClicked()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
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
                is PostViewState.ProgressState.Error -> ErrorView(
                    message = progressState.description,
                    onRepeatClick = {
                        //    component.onRepeatClicked()
                    }
                )

                PostViewState.ProgressState.Init,
                PostViewState.ProgressState.Loading -> LoaderView()

                is PostViewState.ProgressState.Loaded ->
                    CommentsView(state.comments)
            }
        }
    }
}

@Composable
private fun CommentsView(comments: ImmutableList<CommentItem>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(comments) { commentItem ->
            CommentView(commentItem.comment)
        }
    }
}

@Composable
private fun CommentView(comment: Comment, isReply: Boolean = false) {
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.onPrimary)
            .padding(8.dp)
    ) {
        comment.author.avatarUrl?.let { avatarUrl ->
            Image(
                modifier = Modifier
                    .size(if (isReply) 36.dp else 48.dp)
                    .clip(CircleShape),
                painter = rememberImagePainter(avatarUrl),
                contentDescription = "url",
            )
            HorizontalSpacer(8.dp)
        }
        Column {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = comment.author.name,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )
            VerticalSpacer(4.dp)
            comment.content.forEach { postData ->
                ContentView(postData, {})
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = comment.createdAtText,
                style = MaterialTheme.typography.bodySmall
            )

            comment.replies.forEach { reply ->
                CommentView(reply, isReply = true)
            }
        }
    }
}