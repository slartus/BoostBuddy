package ru.slartus.boostbuddy.ui.screens.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.components.main.MainComponent
import ru.slartus.boostbuddy.components.main.title
import ru.slartus.boostbuddy.ui.common.BackHandlerEffect
import ru.slartus.boostbuddy.ui.screens.FeedScreen
import ru.slartus.boostbuddy.ui.screens.SubscribesScreen
import ru.slartus.boostbuddy.ui.screens.TopAppBar

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
    NavigationDrawer(
        activeComponent = activeComponent,
        drawerState = drawerState,
        onItemClick = { item ->
            component.onNavigationItemClick(item)
            scope.launch {
                drawerState.close()
            }
        }
    ) {
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
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                Children(
                    modifier = Modifier.weight(1F).consumeWindowInsets(WindowInsets.navigationBars),
                    component = component,
                )
            }
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

//
//@Composable
//private fun BottomBar(
//    activeComponent: MainComponent.Child) {
//    BottomNavigation(
//        modifier = Modifier
//            .fillMaxWidth()
//            .navigationBarsPadding(),
//        backgroundColor = MaterialTheme.colorScheme.onPrimary,
//        elevation = 4.dp,
//    ) {
//        MainViewNavigationItem.entries.forEach {item->
//            BottomNavigationItem(
//                selectedContentColor = MaterialTheme.colorScheme.primary,
//                selected = activeComponent is MainComponent.Child.FeedChild,
//                onClick = component::onFeedTabClicked,
//                icon = {
//                    Icon(
//                        imageVector = Icons.Default.RssFeed,
//                        contentDescription = "Feed",
//                    )
//                },
//            )
//        }
//        BottomNavigationItem(
//            selectedContentColor = MaterialTheme.colorScheme.primary,
//            selected = activeComponent is MainComponent.Child.FeedChild,
//            onClick = component::onFeedTabClicked,
//            icon = {
//                Icon(
//                    imageVector = Icons.Default.RssFeed,
//                    contentDescription = "Feed",
//                )
//            },
//        )
//        BottomNavigationItem(
//            selectedContentColor = MaterialTheme.colorScheme.primary,
//            selected = activeComponent is MainComponent.Child.SubscribesChild,
//            onClick = component::onSubscribesTabClicked,
//            icon = {
//                Icon(
//                    imageVector = Icons.Default.Subscriptions,
//                    contentDescription = "Subscribes",
//                )
//            },
//        )
//    }
//}