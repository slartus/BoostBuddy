package ru.slartus.boostbuddy.navigation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


interface NavigationRouter {
    fun navigateTo(action: NavigationAction)
    fun actionInvoked()
    val screensStack: Flow<NavigationAction?>
}

fun NavigationRouter.navigateTo(screen: Screen) = navigateTo(ScreenAction(screen))

sealed interface NavigationAction
class ScreenAction(val screen: Screen) : NavigationAction

internal class NavigationRouterImpl : NavigationRouter {
    private val actionsBus: MutableStateFlow<NavigationAction?> = MutableStateFlow(
        null
    )

    override fun navigateTo(action: NavigationAction) {
        actionsBus.value = action
    }

    override fun actionInvoked() {
        actionsBus.value = null
    }

    override val screensStack: Flow<NavigationAction?>
        get() = actionsBus.asStateFlow()
}