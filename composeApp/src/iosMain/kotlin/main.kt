import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import ru.slartus.boostbuddy.components.RootComponent
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration
import ru.slartus.boostbuddy.ui.screens.RootScreen
import ru.slartus.boostbuddy.utils.IosDirectDependencies
import ru.slartus.boostbuddy.utils.UnauthorizedException

fun MainViewController(
    rootComponent: RootComponent
): UIViewController{
    IosDirectDependencies.exceptionsHandler.registerHandler { error ->
        when (error) {
            is UnauthorizedException -> {
                rootComponent.showAuthorizeComponent()
                true
            }

            else -> {
                false
            }
        }
    }
    return ComposeUIViewController {
        CompositionLocalProvider(
            LocalPlatformConfiguration provides IosDirectDependencies.platformConfiguration
        ) {
            RootScreen(rootComponent)
        }
    }
}
