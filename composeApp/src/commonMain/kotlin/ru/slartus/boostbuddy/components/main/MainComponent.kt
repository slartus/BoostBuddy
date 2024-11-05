package ru.slartus.boostbuddy.components.main

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.components.feed.FeedComponent
import ru.slartus.boostbuddy.components.feed.FeedComponentImpl
import ru.slartus.boostbuddy.components.subscribes.SubscribesComponent
import ru.slartus.boostbuddy.components.subscribes.SubscribesComponentImpl
import ru.slartus.boostbuddy.components.top_bar.TopBarComponent
import ru.slartus.boostbuddy.components.top_bar.TopBarComponentImpl
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.utils.unauthorizedError

@Stable
interface MainComponent {
    val feedComponent: FeedComponent
    val subscribesComponent: SubscribesComponent

    val topBarComponent: TopBarComponent
    fun onNavigationItemClick(item: MainViewNavigationItem)

}

internal class MainComponentImpl(
    componentContext: ComponentContext,
) : BaseComponent<Unit, Unit>(
    componentContext,
    Unit
), MainComponent {
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val navigation = StackNavigation<Config>()
    override val feedComponent: FeedComponent = feedComponent(componentContext)
    override val subscribesComponent: SubscribesComponent = subscribesComponent(componentContext)

    override val topBarComponent: TopBarComponent = TopBarComponentImpl(
        componentContext,
        onRefresh = { refresh() }
    )

    init {
        checkToken()
    }

    private fun checkToken() {
        scope.launch {
            if (settingsRepository.getAccessToken() == null)
                unauthorizedError()
        }
    }

    private fun feedComponent(componentContext: ComponentContext): FeedComponent =
        FeedComponentImpl(componentContext)

    private fun subscribesComponent(componentContext: ComponentContext): SubscribesComponent =
        SubscribesComponentImpl(componentContext)

    override fun onNavigationItemClick(item: MainViewNavigationItem) {
        val config = when (item) {
            MainViewNavigationItem.Feed -> Config.Feed
            MainViewNavigationItem.Subscribes -> Config.Subscribes
        }
        navigation.bringToFront(config)
    }

    private fun refresh() {
        feedComponent.refresh()
        subscribesComponent.refresh()
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Subscribes : Config

        @Serializable
        data object Feed : Config
    }
}