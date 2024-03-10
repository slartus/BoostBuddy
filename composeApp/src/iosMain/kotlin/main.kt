import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import ru.slartus.boostbuddy.components.RootComponent
import ru.slartus.boostbuddy.ui.screens.RootScreen

fun MainViewController(
    rootComponent: RootComponent
): UIViewController = ComposeUIViewController { RootScreen(rootComponent) }
