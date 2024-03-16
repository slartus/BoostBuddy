package ru.slartus.boostbuddy.utils


expect class Permissions(platformConfiguration: PlatformConfiguration) {
    suspend fun providePermission(permission: Permission)
    suspend fun isPermissionGranted(permission: Permission): Boolean
    suspend fun getPermissionState(permission: Permission): PermissionState
    fun openAppSettings()
}

enum class Permission {
    InstallApplication
}

enum class PermissionState {
    NotDetermined,
    Granted,
    Denied,
    DeniedAlways
}

open class DeniedException(
    val permission: Permission,
    message: String? = null,
) : Exception(message)

class DeniedAlwaysException(
    permission: Permission,
    message: String? = null,
) : DeniedException(permission, message)

class RequestCanceledException(
    val permission: Permission,
    message: String? = null,
) : Exception(message)