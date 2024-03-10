package ru.slartus.boostbuddy.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import ru.slartus.boostbuddy.components.RootComponent
import ru.slartus.boostbuddy.ui.screens.blog.BlogScreen
import ru.slartus.boostbuddy.ui.theme.AppTheme
import ru.slartus.boostbuddy.ui.theme.LocalThemeIsDark


@Composable
fun RootScreen(component: RootComponent, modifier: Modifier = Modifier) {
    val state by component.viewStates.subscribeAsState()
    AppTheme(state.darkMode) {
        var isDarkState by LocalThemeIsDark.current
        isDarkState = state.darkMode ?: isDarkState
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