package ru.slartus.boostbuddy.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

object BottomViewDefaults {
    val horizontalPadding = 16.dp
    val verticalPadding = 12.dp
    val iconSize = 24.dp
    val spacing = 12.dp
    val titleBottomMargin = 16.dp
}

@Composable
fun BottomView(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            trailingContent?.invoke(this)
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
fun CheckboxListItem(
    text: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(BottomViewDefaults.verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null, // handled by row click
            modifier = Modifier.size(BottomViewDefaults.iconSize)
        )
        Spacer(Modifier.width(BottomViewDefaults.spacing))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun IconTextListItem(
    text: String,
    icon: ImageVector? = null,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(BottomViewDefaults.verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconContent(icon = icon)
        Spacer(Modifier.width(BottomViewDefaults.spacing))
        TextContent(modifier = Modifier.weight(1f), text = text, selected = selected)
        SelectionIndicator(selected = selected)
    }
}

@Composable
private fun IconContent(icon: ImageVector?) {
    if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(BottomViewDefaults.iconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Spacer(Modifier.size(BottomViewDefaults.iconSize))
    }
}

@Composable
private fun TextContent(text: String, selected: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun SelectionIndicator(selected: Boolean) {
    if (selected) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun LoadingListItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(BottomViewDefaults.iconSize),
            strokeWidth = 2.dp
        )
    }
}