package ru.slartus.boostbuddy.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    initialFrom: Clock?,
    initialTo: Clock?,
    onDateRangeSelected: (Clock, Clock) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val snackState = remember { SnackbarHostState() }
    val snackScope = rememberCoroutineScope()

    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialFrom?.toEpochMilliseconds(),
        initialSelectedEndDateMillis = initialTo?.toEpochMilliseconds()
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Top
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            val start = state.selectedStartDateMillis ?: state.selectedEndDateMillis
                            val end = state.selectedEndDateMillis ?: state.selectedStartDateMillis
                            if (start != null && end != null) {
                                val from = Clock.fromEpochMilliseconds(start)
                                val to = Clock.fromEpochMilliseconds(end)
                                onDateRangeSelected(from, to)
                            } else {
                                snackScope.launch {
                                    snackState.showSnackbar("Выберите хотя бы одну дату")
                                }
                            }
                        },
                        enabled = state.selectedEndDateMillis != null
                    ) {
                        Text("Применить")
                    }
                    if (initialTo != null || initialFrom != null) {
                        TextButton(
                            onClick = {
                                onReset()
                            },
                            enabled = state.selectedEndDateMillis != null
                        ) {
                            Text("Сбросить")
                        }
                    }
                }

                DateRangePicker(
                    state = state,
                    modifier = Modifier.weight(1f),
                    title = {
                        Text(
                            "Выберите диапазон",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                )
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                SnackbarHost(
                    hostState = snackState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

// Extensions for Clock <-> milliseconds conversion
fun Clock.toEpochMilliseconds(): Long = this.now().toEpochMilliseconds()

fun Clock.Companion.fromEpochMilliseconds(millis: Long): Clock = object : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(millis)
}

// Null-safe extensions
fun Clock?.toEpochMilliseconds(): Long? = this?.toEpochMilliseconds()