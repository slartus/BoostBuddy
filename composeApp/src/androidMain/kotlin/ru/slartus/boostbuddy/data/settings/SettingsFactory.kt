package ru.slartus.boostbuddy.data.settings

import android.content.Context
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import ru.slartus.boostbuddy.utils.PlatformConfiguration

internal actual class SettingsFactory actual constructor(private val platformConfiguration: PlatformConfiguration) {

    /**
     * Мигрированные с settings-noarg настройки, чтоб избежать ошибки с попыткой доступа раньше,
     * чем отработает androidx.startup.InitializationProvider
     * https://github.com/russhwolf/multiplatform-settings/blob/main/multiplatform-settings-no-arg/src/androidMain/kotlin/com/russhwolf/settings/NoArg.kt
     */
    actual fun createDefault(): ObservableSettings {
        val name = "${platformConfiguration.androidContext.packageName}_preferences"
        val delegate = platformConfiguration.androidContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        return SharedPreferencesSettings(delegate)
    }

}