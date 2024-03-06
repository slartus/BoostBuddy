package ru.slartus.boostbuddy.data.settings

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults
import ru.slartus.boostbuddy.utils.PlatformConfiguration

internal actual class SettingsFactory actual constructor(val platformConfiguration: PlatformConfiguration) {

    actual fun createDefault(): Settings {
        return NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    }
}