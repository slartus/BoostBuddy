package ru.slartus.boostbuddy.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import ru.slartus.boostbuddy.components.RootComponent
import ru.slartus.boostbuddy.ui.theme.AppTheme


@Composable
fun RootScreen(component: RootComponent, modifier: Modifier = Modifier) {
    AppTheme {
        Children(
            stack = component.stack,
            modifier = modifier,
            animation = stackAnimation(fade()),
        ) {
            when (val child = it.instance) {
                is RootComponent.Child.AuthChild -> AuthScreen(child.component)
                is RootComponent.Child.SubscribesChild -> SubscribesScreen(child.component)
                is RootComponent.Child.BlogChild -> BlogScreen(child.component)
                is RootComponent.Child.VideoChild -> VideoScreen(child.component)
            }
        }
    }
}