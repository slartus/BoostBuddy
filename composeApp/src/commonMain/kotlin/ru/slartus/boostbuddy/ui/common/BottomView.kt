package ru.slartus.boostbuddy.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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