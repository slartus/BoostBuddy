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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.seiko.imageloader.rememberImagePainter
import ru.slartus.boostbuddy.components.subscribes.SubscribesComponent
import ru.slartus.boostbuddy.components.subscribes.SubscribesViewState
import ru.slartus.boostbuddy.data.repositories.Blog
import ru.slartus.boostbuddy.ui.widgets.ErrorView
import ru.slartus.boostbuddy.ui.widgets.LoaderView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscribesScreen(component: SubscribesComponent) {
    val state = component.viewStates.subscribeAsState().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Подписки") },
                actions = {
                    IconButton(onClick = { component.onLogoutClicked() }) {
                        Icon(
                            imageVector = Icons.Filled.Logout,
                            contentDescription = "Logout"
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Center
        ) {
            when (val progressState = state.progressProgressState) {
                is SubscribesViewState.ProgressState.Error -> ErrorView(
                    message = progressState.description,
                    onRepeatClick = { component.onRepeatClicked() }
                )

                SubscribesViewState.ProgressState.Init,
                SubscribesViewState.ProgressState.Loading -> LoaderView()

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
        val dialogSlot by component.dialogSlot.subscribeAsState()
        dialogSlot.child?.instance?.also { logoutComponent ->
            LogoutDialogView(
                modifier = Modifier.padding(innerPadding),
                onDismissClicked = { logoutComponent.onDismissed() },
                onAcceptClicked = { logoutComponent.onAcceptClicked() },
                onCancelClicked = { logoutComponent.onCancelClicked() },
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogoutDialogView(
    onAcceptClicked: () -> Unit,
    onCancelClicked: () -> Unit,
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
            Text(
                modifier = Modifier.fillMaxWidth()
                    .clickable { onAcceptClicked() }
                    .padding(16.dp),
                text = "Выйти из аккаунта"
            )
            Text(
                modifier = Modifier.fillMaxWidth()
                    .clickable { onCancelClicked() }
                    .padding(16.dp),
                text = "Отмена"
            )
            Spacer(modifier = Modifier.height(56.dp))
        }
    }
}