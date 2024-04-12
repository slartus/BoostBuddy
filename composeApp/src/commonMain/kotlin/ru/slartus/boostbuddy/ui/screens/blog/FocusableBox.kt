package ru.slartus.boostbuddy.ui.screens.blog

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp

@Composable
internal fun FocusableBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier
        .then(
            if (focused) Modifier.border(0.5.dp, MaterialTheme.colorScheme.primary)
            else Modifier
        )
        .onFocusChanged { focused = it.isFocused }
        .focusable()) {
        content()
    }
}

@Composable
internal fun FocusableRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Row(modifier = modifier
        .then(
            if (focused) Modifier.border(0.5.dp, MaterialTheme.colorScheme.primary)
            else Modifier
        )
        .onFocusChanged { focused = it.isFocused }
        .focusable(),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment
    ) {
        content()
    }
}

@Composable
internal fun FocusableColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier
        .then(
            if (focused) Modifier.border(0.5.dp, MaterialTheme.colorScheme.primary)
            else Modifier
        )
        .onFocusChanged { focused = it.isFocused }
        .focusable()) {
        content()
    }
}