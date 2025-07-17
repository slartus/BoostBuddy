package ru.slartus.boostbuddy.components.top_bar

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import io.github.aakira.napier.Napier
import ru.slartus.boostbuddy.components.BaseComponent
import ru.slartus.boostbuddy.data.Inject
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
}

internal class TopBarComponentImpl(
    componentContext: ComponentContext,
    private val onRefresh: () -> Unit
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
        navigationRouter.navigateTo(NavigationTree.AppSettings)
    }

    override fun onFeedbackClicked() {
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
            Napier.e("onFeedbackClicked", error)
            navigationRouter.navigateTo(
                NavigationTree.Qr(
                    title = "Обсудить на форуме",
                    url = FORUM_URL
                )
            )
        }
    }

    companion object {
        const val FORUM_URL = "https://4pda.to/forum/index.php?showtopic=1085976"
    }

}