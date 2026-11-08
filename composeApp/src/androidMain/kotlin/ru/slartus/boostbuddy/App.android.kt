package ru.slartus.boostbuddy

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.LifecycleOwner
import com.arkivanov.decompose.defaultComponentContext
import io.appmetrica.analytics.AppMetrica
import ru.slartus.boostbuddy.components.RootComponent
import ru.slartus.boostbuddy.components.RootComponentImpl
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.analytic.AppMetricaAnalyticsTracker
import ru.slartus.boostbuddy.data.analytic.analytics
import ru.slartus.boostbuddy.data.log.debugLogBuild
import ru.slartus.boostbuddy.data.pairing.AndroidLanAddressResolver
import ru.slartus.boostbuddy.data.pairing.CioPairingReceiver
import ru.slartus.boostbuddy.data.pairing.CioPairingSender
import ru.slartus.boostbuddy.data.pairing.PairingLink
import ru.slartus.boostbuddy.ui.common.LocalPlatformConfiguration
import ru.slartus.boostbuddy.ui.screens.RootScreen
import ru.slartus.boostbuddy.utils.GlobalExceptionHandlersChain
import ru.slartus.boostbuddy.utils.Permissions
import ru.slartus.boostbuddy.utils.Platform
import ru.slartus.boostbuddy.utils.PlatformConfiguration
import ru.slartus.boostbuddy.utils.PlatformDataConfiguration
import ru.slartus.boostbuddy.utils.UnauthorizedException

class AndroidApp : Application() {
    companion object {
        lateinit var INSTANCE: AndroidApp
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        debugLogBuild()
        PlatformDataConfiguration.createDependenciesTree(
            PlatformConfiguration(this, getPlatform()),
            buildList {
                if (!BuildConfig.DEBUG) {
                    add(AppMetricaAnalyticsTracker(this@AndroidApp))
                }
            }
        )
        PlatformDataConfiguration.bindPairingSender { CioPairingSender(allowLoopback = BuildConfig.DEBUG) }
        PlatformDataConfiguration.bindPairingReceiver {
            CioPairingReceiver(
                allowLoopback = BuildConfig.DEBUG,
                lanAddress = AndroidLanAddressResolver(this)::resolve,
                monotonicClock = SystemClock::elapsedRealtime,
            )
        }
    }

    private fun getPlatform(): Platform {
        return if (isDirectToTV()) Platform.AndroidTV else Platform.Android
    }

    private fun isDirectToTV(): Boolean {
        return (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
                || packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK))
    }
}

class AppActivity : BaseComponentActivity()

class TvAppActivity : BaseComponentActivity()

open class BaseComponentActivity : ComponentActivity() {
    private lateinit var root: RootComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val root = RootComponentImpl(componentContext = defaultComponentContext())
        this.root = root

        Inject.instance<GlobalExceptionHandlersChain>().registerHandler { error ->
            when (error) {
                is UnauthorizedException -> {
                    root.showAuthorizeComponent()
                    true
                }

                else -> {
                    analytics.reportUnhandledException(error)
                    root.onErrorReceived(error)
                    true
                }
            }
        }
        setContent {
            val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
            LaunchedEffect(lifecycleOwner) {
                Inject.instance<Permissions>().bind(lifecycleOwner.lifecycle)
            }
            CompositionLocalProvider(
                LocalPlatformConfiguration provides Inject.instance<PlatformConfiguration>()
            ) {
                RootScreen(component = root, modifier = Modifier.fillMaxSize())
            }
        }
        consumeDeepLink(intent, handle = savedInstanceState == null)
        if (!BuildConfig.DEBUG) AppMetrica.reportAppOpen(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeDeepLink(intent, handle = true)
    }

    private fun consumeDeepLink(intent: Intent, handle: Boolean) {
        if (intent.action != Intent.ACTION_VIEW) return
        val data = intent.data ?: return
        val isPairingLink = data.scheme.equals(PairingLink.APP_SCHEME, ignoreCase = true) &&
                data.host.equals(PairingLink.APP_HOST, ignoreCase = true)
        if (!isPairingLink) return
        intent.data = null
        setIntent(intent)
        val launchedFromHistory = (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
        if (handle && !launchedFromHistory) root.onDeepLink(data.toString())
    }
}