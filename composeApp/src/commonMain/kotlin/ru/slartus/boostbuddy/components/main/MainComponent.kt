package ru.slartus.boostbuddy.components.main

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.components.feed.FeedComponent
import ru.slartus.boostbuddy.components.feed.FeedComponentImpl
import ru.slartus.boostbuddy.components.subscribes.SubscribesComponent
import ru.slartus.boostbuddy.components.subscribes.SubscribesComponentImpl
import ru.slartus.boostbuddy.components.top_bar.TopBarComponent
import ru.slartus.boostbuddy.components.top_bar.TopBarComponentImpl

@Stable
interface MainComponent {
    val stack: Value<ChildStack<*, Child>>

    val topBarComponent: TopBarComponent
    fun onSubscribesTabClicked()
    fun onFeedTabClicked()

    sealed class Child(val title: String) {
        class SubscribesChild(val component: SubscribesComponent) : Child("Подписки")
        class FeedChild(val component: FeedComponent) : Child("Лента")
    }
}

internal class MainComponentImpl(
    componentContext: ComponentContext,
) : BaseComponent<Unit, Unit>(
    componentContext,
    Unit
), MainComponent {
    private val navigation = StackNavigation<Config>()

    override val topBarComponent: TopBarComponent = TopBarComponentImpl(
        componentContext,
        onRefresh = { refresh() }
    )

    override val stack: Value<ChildStack<*, MainComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Feed,
            childFactory = ::child,
        )

    private fun child(config: Config, componentContext: ComponentContext): MainComponent.Child =
        when (config) {
            is Config.Subscribes ->
                MainComponent.Child.SubscribesChild(subscribesComponent(componentContext))

            Config.Feed ->
                MainComponent.Child.FeedChild(feedComponent(componentContext))
        }

    private fun feedComponent(componentContext: ComponentContext): FeedComponent =
        FeedComponentImpl(componentContext)

    private fun subscribesComponent(componentContext: ComponentContext): SubscribesComponent =
        SubscribesComponentImpl(componentContext)

    override fun onSubscribesTabClicked() {
        navigation.bringToFront(Config.Subscribes)
    }

    override fun onFeedTabClicked() {
        navigation.bringToFront(Config.Feed)
    }

    private fun refresh() {
        stack.value.items
            .map { it.instance }
            .forEach { child ->
                when (child) {
                    is MainComponent.Child.SubscribesChild -> child.component.refresh()
                    is MainComponent.Child.FeedChild -> child.component.refresh()
                }
            }
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Subscribes : Config
        @Serializable
        data object Feed : Config
    }

}