package ru.slartus.boostbuddy

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.defaultComponentContext
import ru.slartus.boostbuddy.components.RootComponentImpl
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.createDependenciesTree
import ru.slartus.boostbuddy.screens.RootScreen
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

class AppActivity : ComponentActivity() {
    private val globalExceptionHandlersChain by Inject.lazy<GlobalExceptionHandlersChain>()
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
            RootScreen(component = root, modifier = Modifier.fillMaxSize())
        }
    }
}