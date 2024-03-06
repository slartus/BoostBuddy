package ru.slartus.boostbuddy.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popTo
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.data.repositories.Blog

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    // It's possible to pop multiple screens at a time on iOS
    fun onBackClicked(toIndex: Int)

    // Defines all possible child components
    sealed class Child {
        class AuthChild(val component: AuthComponent) : Child()
        class SubscribesChild(val component: SubscribesComponent) : Child()
        class BlogChild(val component: BlogComponent) : Child()
    }
}

class RootComponentImpl(
    componentContext: ComponentContext,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Auth,
            handleBackButton = true,
            childFactory = ::child,
        )

    private fun child(config: Config, componentContext: ComponentContext): RootComponent.Child =
        when (config) {
            is Config.Auth -> RootComponent.Child.AuthChild(authComponent(componentContext))
            is Config.Subscribes -> RootComponent.Child.SubscribesChild(
                subscribesComponent(
                    componentContext
                )
            )

            is Config.BlogConfig -> RootComponent.Child.BlogChild(
                blogComponent(
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

    private fun subscribesComponent(componentContext: ComponentContext): SubscribesComponent =
        SubscribesComponentImpl(
            componentContext = componentContext,
            onItemSelected = {
                navigation.push(Config.BlogConfig(blog = it.blog))
            }
        )

    private fun blogComponent(
        componentContext: ComponentContext,
        config: Config.BlogConfig
    ): BlogComponent =
        BlogComponentImpl(
            componentContext = componentContext,
            blog = config.blog,
            onItemSelected = {
                //navigation.push(Config.BlogConfig(blog = it.blog))
            }
        )

    override fun onBackClicked(toIndex: Int) {
        navigation.popTo(index = toIndex)
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Auth : Config

        @Serializable
        data object Subscribes : Config

        @Serializable
        data class BlogConfig(val blog: Blog) : Config
    }
}