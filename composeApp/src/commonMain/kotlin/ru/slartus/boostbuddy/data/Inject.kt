package ru.slartus.boostbuddy.data

import org.kodein.di.DI
import org.kodein.di.LazyDelegate
import org.kodein.di.bindSingleton
import org.kodein.di.conf.ConfigurableDI
import org.kodein.di.direct
import org.kodein.di.instance
import ru.slartus.boostbuddy.data.ktor.buildHttpClient
import ru.slartus.boostbuddy.data.repositories.AuthRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.SubscribesRepository
import ru.slartus.boostbuddy.data.settings.SettingsFactory
import ru.slartus.boostbuddy.utils.PlatformConfiguration

object Inject {
    val diLazy: DI get() = _di.direct.lazy

    @Suppress("VARIABLE_IN_SINGLETON_WITHOUT_THREAD_LOCAL")
    private lateinit var _di: ConfigurableDI

    fun createDependenciesTree(init: DI.MainBuilder.() -> Unit) {
        _di = ConfigurableDI(true).addConfig(init)
    }

    inline fun <reified T> lazy(): LazyDelegate<T> {
        return diLazy.instance()
    }
}

fun createDependenciesTree(platformConfiguration: PlatformConfiguration) {
    Inject.createDependenciesTree {
        bindSingleton { platformConfiguration }
        bindSingleton { buildHttpClient(true) }
        bindSingleton { AuthRepository(httpClient = instance()) }
        bindSingleton { SettingsFactory(platformConfiguration = instance()).createDefault() }
        bindSingleton { SettingsRepository(settings = instance()) }
        bindSingleton { SubscribesRepository(httpClient = instance()) }
    }
}