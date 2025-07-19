package ru.slartus.boostbuddy.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopAppBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    onRefreshClick: () -> Unit,
    onFilterClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
) {
    var searchState by remember { mutableStateOf(SearchState()) }
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
                    focusRequester = focusRequester,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                title()
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
                navigationIcon()
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

            actions()
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
private fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
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
