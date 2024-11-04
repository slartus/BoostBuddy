package ru.slartus.boostbuddy.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.main.MainComponent
import ru.slartus.boostbuddy.components.main.MainViewNavigationItem
import ru.slartus.boostbuddy.ui.common.BackHandlerEffect
import ru.slartus.boostbuddy.ui.screens.FeedScreen
import ru.slartus.boostbuddy.ui.screens.SubscribesScreen
import ru.slartus.boostbuddy.ui.screens.TopAppBar

@Composable
internal fun MainScreen(component: MainComponent) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    BackHandlerEffect(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "Лента",
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
        ModalNavigationDrawer(
            modifier = Modifier.padding(innerPaddings),
            drawerState= drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerShape = RoundedCornerShape(0.dp)
                ) {
                    SubscribesScreen(component.subscribesComponent)
                }
            }
        ) {
            FeedScreen(component.feedComponent)
        }
    }
}

internal val MainViewNavigationItem.icon: ImageVector
    get() = when (this) {
        MainViewNavigationItem.Feed -> Icons.Default.RssFeed
        MainViewNavigationItem.Subscribes -> Icons.Default.Subscriptions
    }