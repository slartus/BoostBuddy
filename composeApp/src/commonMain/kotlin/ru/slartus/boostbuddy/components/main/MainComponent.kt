package ru.slartus.boostbuddy.components.main

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.components.subscribes.SubscribesComponent
import ru.slartus.boostbuddy.components.subscribes.SubscribesComponentImpl

@Stable
interface MainComponent {
    val stack: Value<ChildStack<*, Child>>

    fun onSubscribesTabClicked()

    sealed class Child {
        class SubscribesChild(val component: SubscribesComponent) : Child()
    }
}

internal class MainComponentImpl(
    componentContext: ComponentContext,
) : BaseComponent<MainViewState, Any>(
    componentContext,
    MainViewState()
), MainComponent {
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, MainComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Subscribes,
            childFactory = ::child,
        )

    private fun child(config: Config, componentContext: ComponentContext): MainComponent.Child =
        when (config) {
            is Config.Subscribes ->
                MainComponent.Child.SubscribesChild(subscribesComponent(componentContext))
        }

    private fun subscribesComponent(componentContext: ComponentContext): SubscribesComponent =
        SubscribesComponentImpl(
            componentContext = componentContext,
            onBackClicked = {
                navigation.pop()
            }
        )

    override fun onSubscribesTabClicked() {
        navigation.bringToFront(Config.Subscribes)
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Subscribes : Config
    }

}