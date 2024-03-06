import androidx.compose.ui.window.ComposeUIViewController
import ru.slartus.boostbuddy.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
