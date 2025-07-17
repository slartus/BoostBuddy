package ru.slartus.boostbuddy.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
internal fun BottomView(
    title: String,
    content: @Composable () -> Unit
) {
    Column(Modifier) {
        Text(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(16.dp))
        content()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
internal fun CheckboxBottomViewItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { newCheckedState ->
                onCheckedChange(newCheckedState)
            },
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


@Composable
internal fun TextBottomViewItem(
    text: String,
    icon: ImageVector? = null,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (selected) {
                    Modifier.background(color = MaterialTheme.colorScheme.secondaryContainer)
                } else {
                    Modifier
                }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(24.dp)) {
            if (icon != null) {
                Icon(
                    modifier = Modifier.fillMaxSize(),
                    imageVector = icon,
                    contentDescription = "Icon"
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}