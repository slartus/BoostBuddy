package ru.slartus.boostbuddy.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private fun interface Observer {
    fun onEvent(observer: Observer, event: Lifecycle.Event)
}

actual class Permissions actual constructor(platformConfiguration: PlatformConfiguration) {
    private val applicationContext: Context = platformConfiguration.androidContext
    private val lifecycleObservers: MutableList<Observer> = mutableListOf()

    actual suspend fun providePermission(permission: Permission) {
        when (permission) {
            Permission.InstallApplication -> provideInstallApplicationPermission()
        }
    }

    actual suspend fun isPermissionGranted(permission: Permission): Boolean =
        getPermissionState(permission) == PermissionState.Granted

    private suspend fun provideInstallApplicationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            suspendCoroutine { continuation ->
                val observer = Observer { observer, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        if (getInstallApplicationState() == PermissionState.Granted)
                            continuation.resume(Unit)
                        else
                            continuation.resumeWithException(DeniedException(Permission.InstallApplication))
                        lifecycleObservers.remove(observer)
                    } else if (event == Lifecycle.Event.ON_DESTROY) {
                        lifecycleObservers.remove(observer)
                    }
                }

                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${applicationContext.packageName}")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                applicationContext.startActivity(intent)
                lifecycleObservers.add(observer)
            }
        }
    }

    actual suspend fun getPermissionState(permission: Permission): PermissionState =
        when (permission) {
            Permission.InstallApplication -> getInstallApplicationState()
        }

    private fun getInstallApplicationState(): PermissionState =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (applicationContext.packageManager.canRequestPackageInstalls()) PermissionState.Granted else PermissionState.DeniedAlways
        } else {
            PermissionState.Granted
        }

    actual fun openAppSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", applicationContext.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        applicationContext.startActivity(intent)
    }

    fun bind(lifecycle: Lifecycle) {
        val observer = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                lifecycleObservers.forEach { observer -> observer.onEvent(observer, event) }
                if (event == Lifecycle.Event.ON_DESTROY) {
                    source.lifecycle.removeObserver(this)
                }
            }
        }
        lifecycle.addObserver(observer)
    }
}