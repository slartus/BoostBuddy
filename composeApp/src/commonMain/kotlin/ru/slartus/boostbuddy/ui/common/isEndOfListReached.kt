package ru.slartus.boostbuddy.ui.common

import androidx.compose.foundation.lazy.LazyListState

fun LazyListState.isEndOfListReached(visibleThreshold: Int = 0): Boolean {
    val lastItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return false
    return (layoutInfo.totalItemsCount - 1) <= (lastItemIndex + visibleThreshold)
}