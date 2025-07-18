package ru.slartus.boostbuddy.data.settings

import com.russhwolf.settings.ObservableSettings
import ru.slartus.boostbuddy.utils.PlatformConfiguration

internal expect class SettingsFactory(platformConfiguration: PlatformConfiguration) {
    fun createDefault(): ObservableSettings
}