package ru.slartus.boostbuddy.components.top_bar

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.components.filter.Filter
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.analytic.analytics
import ru.slartus.boostbuddy.data.log.logger
import ru.slartus.boostbuddy.navigation.NavigationRouter
import ru.slartus.boostbuddy.navigation.NavigationTree
import ru.slartus.boostbuddy.navigation.navigateTo
import ru.slartus.boostbuddy.utils.Platform
import ru.slartus.boostbuddy.utils.PlatformConfiguration

@Stable
interface TopBarComponent {
    fun onLogoutClicked()
    fun onRefreshClicked()
    fun onSettingsClicked()
    fun onFeedbackClicked()
    fun onFilterClicked()
    fun onSearchQueryChange(query: String)
}

internal class TopBarComponentImpl(
    componentContext: ComponentContext,
    private var filter: Filter,
    private val onRefresh: () -> Unit,
    private val onFilter: (filter: Filter) -> Unit,
    private val onSearchQuery: (query: String) -> Unit,
) : BaseComponent<Unit, Unit>(
    componentContext,
    Unit
), TopBarComponent {
    private val navigationRouter by Inject.lazy<NavigationRouter>()
    private val platformConfiguration by Inject.lazy<PlatformConfiguration>()
    override fun onLogoutClicked() {
        navigationRouter.navigateTo(NavigationTree.Logout)
    }

    override fun onRefreshClicked() {
        onRefresh()
    }

    override fun onSettingsClicked() {
        analytics.trackEvent("main_menu", mapOf("action" to "open"))
        navigationRouter.navigateTo(NavigationTree.AppSettings)
    }

    override fun onFeedbackClicked() {
        analytics.trackEvent("main_menu", mapOf("action" to "feedback"))
        runCatching {
            when (platformConfiguration.platform) {
                Platform.Android,
                Platform.iOS -> platformConfiguration.openBrowser(FORUM_URL) {
                    navigationRouter.navigateTo(
                        NavigationTree.Qr(
                            title = "Обсудить на форуме",
                            url = FORUM_URL
                        )
                    )
                }

                Platform.AndroidTV -> navigationRouter.navigateTo(
                    NavigationTree.Qr(
                        title = "Обсудить на форуме",
                        url = FORUM_URL
                    )
                )
            }
        }.onFailure { error ->
            logger.e("onFeedbackClicked", error)
            navigationRouter.navigateTo(
                NavigationTree.Qr(
                    title = "Обсудить на форуме",
                    url = FORUM_URL
                )
            )
        }
    }

    override fun onFilterClicked() {
        navigationRouter.navigateTo(
            NavigationTree.Filter(filter) { newFilter ->
                filter = newFilter
                onFilter(newFilter)
            }
        )
    }

    override fun onSearchQueryChange(query: String) {
        onSearchQuery(query)
    }

    companion object {
        const val FORUM_URL = "https://4pda.to/forum/index.php?showtopic=1085976"
    }

}