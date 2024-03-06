package ru.slartus.boostbuddy

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.arkivanov.decompose.defaultComponentContext
import ru.slartus.boostbuddy.components.RootComponentImpl
import ru.slartus.boostbuddy.data.createDependenciesTree
import ru.slartus.boostbuddy.screens.RootScreen
import ru.slartus.boostbuddy.utils.PlatformConfiguration

class AndroidApp : Application() {
    companion object {
        lateinit var INSTANCE: AndroidApp
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        createDependenciesTree(PlatformConfiguration(this))
    }
}

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars.
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Always create the root component outside Compose on the main thread
        val root =
            RootComponentImpl(
                componentContext = defaultComponentContext(),
            )

        setContent {
            MaterialTheme {
                Surface {
                    RootScreen(component = root, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}