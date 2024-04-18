package ru.slartus.boostbuddy.ui.common

import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import kotlin.math.min

@LayoutScopeMarker
@Immutable
internal interface AppColumnScope

@Composable
internal fun AppColumn(
    modifier: Modifier = Modifier,
    content: @Composable AppColumnScope.() -> Unit
) {
    Layout(
        modifier = modifier.clipToBounds(),
        content = {
            AppColumnScopeInstance.content()
        }
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minHeight = 0, maxHeight = 10_000)
        var maxHeight = 0
        val placeables = measurables.map { measurable ->
            val placeable = measurable.measure(childConstraints)
            maxHeight += placeable.height
            placeable
        }

        layout(constraints.maxWidth, min(constraints.maxHeight, maxHeight)) {
            var yPosition = 0

            placeables.filter { it.height > 0 }.forEach { placeable ->
                placeable.placeRelative(x = 0, y = yPosition)

                yPosition += placeable.height
            }
        }
    }
}

internal object AppColumnScopeInstance : AppColumnScope

@Composable
internal fun AppColumnScope.VerticalSpacer(height: Dp) = Spacer(modifier = Modifier.height(height))