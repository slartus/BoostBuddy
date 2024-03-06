package ru.slartus.boostbuddy.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popTo
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize


interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    // It's possible to pop multiple screens at a time on iOS
    fun onBackClicked(toIndex: Int)

    // Defines all possible child components
    sealed class Child {
        class AuthChild(val component: AuthComponent) : Child()
        class SubscribesChild(val component: SubscribesComponent) : Child()
    }
}

class RootComponentImpl(
    componentContext: ComponentContext,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()


    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            initialConfiguration = Config.Auth, // The initial child component is List
            handleBackButton = true, // Automatically pop from the stack on back button presses
            childFactory = ::child,
        )

    private fun child(config: Config, componentContext: ComponentContext): RootComponent.Child =
        when (config) {
            is Config.Auth -> RootComponent.Child.AuthChild(authComponent(componentContext))
            is Config.Subscribes -> RootComponent.Child.SubscribesChild(
                subscribesComponent(
                    componentContext,
                    config
                )
            )
        }

    private fun authComponent(componentContext: ComponentContext): AuthComponent =
        AuthComponentImpl(
            componentContext = componentContext,
            onLogined = {
                navigation.pop {
                    navigation.push(Config.Subscribes)
                }
            },
        )

    private fun subscribesComponent(
        componentContext: ComponentContext,
        config: Config.Subscribes
    ): SubscribesComponent =
        SubscribesComponentImpl(
            componentContext = componentContext,
            onItemSelected = {
                // navigation.push(Config.Details(item = item))
            }
        )

    override fun onBackClicked(toIndex: Int) {
        navigation.popTo(index = toIndex)
    }

    @Parcelize
    private sealed interface Config : Parcelable {
        data object Auth : Config
        data object Subscribes : Config
    }
}