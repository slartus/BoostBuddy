package ru.slartus.boostbuddy.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
expect fun NavigationComponent(
   items: List<NavigationItem>,
   onItemClick: (NavigationItem) -> Unit,
   modifier: Modifier = Modifier,
   content: @Composable () -> Unit
)

data class NavigationItem(
    val icon: ImageVector,
    val label: String,
    val selected: Boolean,
)