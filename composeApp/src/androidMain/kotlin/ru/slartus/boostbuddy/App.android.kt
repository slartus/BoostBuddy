package ru.slartus.boostbuddy

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.defaultComponentContext
import ru.slartus.boostbuddy.components.RootComponentImpl
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.createDependenciesTree
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration
import ru.slartus.boostbuddy.ui.screens.RootScreen
import ru.slartus.boostbuddy.utils.GlobalExceptionHandlersChain
import ru.slartus.boostbuddy.utils.PlatformConfiguration
import ru.slartus.boostbuddy.utils.UnauthorizedException

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

class AppActivity : BaseComponentActivity()

class TvAppActivity : BaseComponentActivity()

open class BaseComponentActivity : ComponentActivity() {
    private val globalExceptionHandlersChain by Inject.lazy<GlobalExceptionHandlersChain>()
    private val platformConfiguration by Inject.lazy<PlatformConfiguration>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = RootComponentImpl(componentContext = defaultComponentContext())

        globalExceptionHandlersChain.registerHandler { error ->
            when (error) {
                is UnauthorizedException -> {
                    root.showAuthorizeComponent()
                    true
                }

                else -> {
                    false
                }
            }
        }
        setContent {
            CompositionLocalProvider(
                LocalPlatformConfiguration provides platformConfiguration
            ) {
                RootScreen(component = root, modifier = Modifier.fillMaxSize())
            }
        }
    }
}