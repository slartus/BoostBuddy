package ru.slartus.boostbuddy.ui.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
internal fun ColumnScope.VerticalSpacer(height: Dp) = Spacer(modifier = Modifier.height(height))

@Composable
internal fun LazyItemScope.VerticalSpacer(height: Dp) = Spacer(modifier = Modifier.height(height))

@Composable
internal fun LazyGridItemScope.VerticalSpacer(height: Dp) = Spacer(modifier = Modifier.height(height))

@Composable
internal fun RowScope.HorizontalSpacer(width: Dp) = Spacer(modifier = Modifier.width(width))

@Composable
internal fun LazyItemScope.HorizontalSpacer(width: Dp) = Spacer(modifier = Modifier.width(width))

@Composable
internal fun LazyGridItemScope.HorizontalSpacer(width: Dp) = Spacer(modifier = Modifier.width(width))