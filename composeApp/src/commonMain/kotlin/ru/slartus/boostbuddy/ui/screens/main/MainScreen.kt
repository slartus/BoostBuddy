package ru.slartus.boostbuddy.ui.screens.main

import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.LayoutDirection
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.main.MainComponent
import ru.slartus.boostbuddy.components.main.MainViewNavigationItem
import ru.slartus.boostbuddy.components.main.title
import ru.slartus.boostbuddy.ui.common.BackHandlerEffect
import ru.slartus.boostbuddy.ui.screens.FeedScreen
import ru.slartus.boostbuddy.ui.screens.SubscribesScreen
import ru.slartus.boostbuddy.ui.screens.TopAppBar
import ru.slartus.boostbuddy.ui.widgets.NavigationComponent
import ru.slartus.boostbuddy.ui.widgets.NavigationItem

@Composable
internal fun MainScreen(component: MainComponent) {
    val stack by component.stack.subscribeAsState()
    val activeComponent = stack.active.instance
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    BackHandlerEffect(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }
    val items = remember(activeComponent.navigationItem) {
        MainViewNavigationItem.entries.map { item ->
            NavigationItem(
                icon = item.icon,
                label = item.title,
                selected = activeComponent.navigationItem == item
            )
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = activeComponent.navigationItem.title,
                component = component.topBarComponent,
                onMenuClick = {
                    scope.launch {
                        if (drawerState.isOpen)
                            drawerState.close()
                        else
                            drawerState.open()
                    }
                }
            )
        },
    ) { innerPaddings ->
        NavigationComponent(
            modifier = Modifier
                .padding(
                    top = innerPaddings.calculateTopPadding(),
                    start = innerPaddings.calculateStartPadding(LayoutDirection.Ltr),
                    end = innerPaddings.calculateEndPadding(LayoutDirection.Ltr)
                ),
            items = items,
            onItemClick = { item ->
                component.onNavigationItemClick(MainViewNavigationItem.entries.single { it.title == item.label })
            }
        ) {
            Children(component)
        }
    }
}

@Composable
private fun Children(component: MainComponent, modifier: Modifier = Modifier) {
    Children(
        stack = component.stack,
        modifier = modifier,
        animation = stackAnimation(fade()),
    ) {
        when (val child = it.instance) {
            is MainComponent.Child.SubscribesChild -> SubscribesScreen(child.component)
            is MainComponent.Child.FeedChild -> FeedScreen(child.component)
        }
    }
}

internal val MainViewNavigationItem.icon: ImageVector
    get() = when (this) {
        MainViewNavigationItem.Feed -> Icons.Default.RssFeed
        MainViewNavigationItem.Subscribes -> Icons.Default.Subscriptions
    }