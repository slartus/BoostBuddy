package ru.slartus.boostbuddy.ui.screens


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.slartus.boostbuddy.components.main.MainComponent


@Composable
internal fun MainScreen(component: MainComponent) {
    val stack by component.stack.subscribeAsState()
    val activeComponent = stack.active.instance
    Scaffold(
        topBar = {
            TopAppBar(title = activeComponent.title, component.topBarComponent)
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Children(
                modifier = Modifier.weight(1F).consumeWindowInsets(WindowInsets.navigationBars),
                component = component,
            )
            BottomBar(component = component, modifier = Modifier.fillMaxWidth())
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
            is MainComponent.Child.SubscribesChild -> SubscribesScreen(component = child.component)
        }
    }
}


@Composable
private fun BottomBar(component: MainComponent, modifier: Modifier = Modifier) {
    val stack by component.stack.subscribeAsState()
    val activeComponent = stack.active.instance

    BottomNavigation(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        backgroundColor = MaterialTheme.colorScheme.onPrimary,
        elevation = 4.dp,
    ) {
        BottomNavigationItem(
            selectedContentColor = MaterialTheme.colorScheme.primary,
            selected = activeComponent is MainComponent.Child.SubscribesChild,
            onClick = component::onSubscribesTabClicked,
            icon = {
                Icon(
                    imageVector = Icons.Default.Subscriptions,
                    contentDescription = "Subscribes",
                )
            },
        )
    }
}