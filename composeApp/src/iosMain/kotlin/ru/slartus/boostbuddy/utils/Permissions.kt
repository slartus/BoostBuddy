package ru.slartus.boostbuddy.utils

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString


actual class Permissions actual constructor(platformConfiguration: PlatformConfiguration) {
    actual suspend fun isPermissionGranted(permission: Permission): Boolean =
        getPermissionState(permission) == PermissionState.Granted

    actual suspend fun providePermission(permission: Permission) {
        when (permission) {
            Permission.InstallApplication -> error("install permission is not defined")
        }
    }

    actual suspend fun getPermissionState(permission: Permission): PermissionState =
        when (permission) {
            Permission.InstallApplication -> PermissionState.Granted
        }

    actual fun openAppSettings() {
        val settingsUrl: NSURL = NSURL.URLWithString(UIApplicationOpenSettingsURLString)!!
        UIApplication.sharedApplication.openURL(settingsUrl)
    }
}