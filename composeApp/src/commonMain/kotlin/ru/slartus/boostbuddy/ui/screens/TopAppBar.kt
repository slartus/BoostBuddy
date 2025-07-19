package ru.slartus.boostbuddy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.top_bar.TopBarComponent

@Composable
internal fun TopAppBar(
    title: String,
    component: TopBarComponent,
    onMenuClick: () -> Unit
) {
    TopAppBarContent(
        title = title,
        onRefreshClick = component::onRefreshClicked,
        onLogoutClick = component::onLogoutClicked,
        onFeedbackClick = component::onFeedbackClicked,
        onSettingsClick = component::onSettingsClicked,
        onFilterClick = component::onFilterClicked,
        onMenuClick = onMenuClick,
        onSearchQueryChange = component::onSearchQueryChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBarContent(
    title: String,
    onMenuClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onFilterClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
) {
    var searchState by remember { mutableStateOf(SearchState()) }
    var showDropDownMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    HandleSearchStateChanges(
        searchState = searchState,
        onSearchStateChange = { searchState = it },
        focusRequester = focusRequester,
        onSearchQueryChange = onSearchQueryChange
    )

    androidx.compose.material3.TopAppBar(
        title = {
            if (searchState.isActive) {
                SearchTextField(
                    query = searchState.query,
                    onQueryChange = { searchState = searchState.copy(query = it) },
                    onClose = { searchState = searchState.copy(query = "") },
                    focusRequester = focusRequester,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(text = title)
            }
        },
        navigationIcon = {
            if (searchState.isActive) {
                IconButton(onClick = { searchState = searchState.copy(query = "", isActive = false) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Закрыть поиск"
                    )
                }
            } else {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Меню"
                    )
                }
            }
        },
        actions = {
            if (searchState.isActive) return@TopAppBar

            SearchAction { searchState = SearchState(isActive = true) }
            IconButton(onClick = onFilterClick) {
                Icon(Icons.Filled.FilterList, "Фильтр")
            }
            IconButton(onClick = onRefreshClick) {
                Icon(Icons.Filled.Refresh, "Обновить")
            }
            AppDropdownMenu(
                expanded = showDropDownMenu,
                onDismiss = { showDropDownMenu = false },
                onSettingsClick = onSettingsClick,
                onFeedbackClick = onFeedbackClick,
                onLogoutClick = onLogoutClick
            )
            DropdownMenuTrigger(
                onClick = { showDropDownMenu = true }
            )
        }
    )
}

@Composable
private fun SearchAction(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Filled.Search, "Поиск")
    }
}

@Composable
private fun DropdownMenuTrigger(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Filled.MoreVert, "Дополнительные действия")
    }
}

@Composable
private fun AppDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Настройки") },
            leadingIcon = { Icon(Icons.Filled.Settings, "Settings") },
            onClick = {
                onDismiss()
                onSettingsClick()
            }
        )
        DropdownMenuItem(
            text = { Text("Обсудить на форуме") },
            leadingIcon = { Icon(Icons.Filled.Feedback, "Feedback") },
            onClick = {
                onDismiss()
                onFeedbackClick()
            }
        )
        DropdownMenuItem(
            text = { Text("Выйти из аккаунта") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, "Logout") },
            onClick = {
                onDismiss()
                onLogoutClick()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .padding(end = 8.dp)
            .focusRequester(focusRequester),
        placeholder = { Text("Поиск...") },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true,
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, "Очистить")
                }
            }
        }
    )
}

@Composable
private fun HandleSearchStateChanges(
    searchState: SearchState,
    onSearchStateChange: (SearchState) -> Unit,
    focusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit
) {
    var searchJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(searchState.query) {
        searchJob?.cancel()

        if (searchState.query.isNotEmpty()) {
            searchJob = launch {
                delay(1000)
                onSearchQueryChange(searchState.query)
                onSearchStateChange(searchState.copy(lastQuery = searchState.query))
            }
        } else if (searchState.lastQuery.isNotEmpty()) {
            onSearchQueryChange("")
            onSearchStateChange(searchState.copy(lastQuery = ""))
        }
    }

    LaunchedEffect(searchState.isActive) {
        if (searchState.isActive) {
            focusRequester.requestFocus()
        }
    }
}

private data class SearchState(
    val isActive: Boolean = false,
    val query: String = "",
    val lastQuery: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogoutDialogView(
    onAcceptClicked: () -> Unit,
    onCancelClicked: () -> Unit,
    onDismissClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissClicked,
        sheetState = sheetState
    ) {
        Column {
            DialogOption(
                text = "Выйти из аккаунта",
                onClick = onAcceptClicked
            )
            DialogOption(
                text = "Отмена",
                onClick = onCancelClicked
            )
        }
    }
}

@Composable
private fun DialogOption(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    )
}