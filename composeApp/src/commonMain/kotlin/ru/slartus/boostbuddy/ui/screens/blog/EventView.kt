package ru.slartus.boostbuddy.ui.screens.blog

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.slartus.boostbuddy.data.repositories.models.Event

@Composable
internal fun EventView(event: Event) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.onPrimary)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FocusableBox {
            Text(
                modifier = Modifier.fillMaxWidth().focusable(),
                text = "Запланирован стрим: ${event.eventTimeText}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}