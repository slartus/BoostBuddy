package ru.slartus.boostbuddy.data.settings

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import platform.Foundation.NSUserDefaults
import ru.slartus.boostbuddy.utils.PlatformConfiguration

internal actual class SettingsFactory actual constructor(val platformConfiguration: PlatformConfiguration) {

    actual fun createDefault(): ObservableSettings {
        return NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    }
}