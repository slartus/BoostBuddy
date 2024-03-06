package ru.slartus.boostbuddy.data

import io.ktor.client.HttpClient
import org.kodein.di.DI
import org.kodein.di.DirectDI
import org.kodein.di.LazyDelegate
import org.kodein.di.bindSingleton
import org.kodein.di.conf.ConfigurableDI
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.type.generic
import ru.slartus.boostbuddy.data.ktor.buildHttpClient
import ru.slartus.boostbuddy.data.repositories.AuthRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.settings.SettingsFactory
import ru.slartus.boostbuddy.utils.PlatformConfiguration

object Inject {

    val di: DirectDI get() = _di.direct

    val diLazy: DI get() = _di.direct.lazy

    @Suppress("VARIABLE_IN_SINGLETON_WITHOUT_THREAD_LOCAL")
    private lateinit var _di: ConfigurableDI

    fun createDependenciesTree(init: DI.MainBuilder.() -> Unit) {
        _di = ConfigurableDI(true).addConfig(init)
    }

    inline fun <reified T> instance(): T {
        return di.instance()
    }

    inline fun <reified T> lazy(): LazyDelegate<T> {
        return diLazy.instance()
    }

    inline fun <reified T : Any> instance(tag: Any? = null): T = di.Instance(generic(), tag)

    // Для мокирования зависимостей графа в тестах
    fun addExtend(newDi: DI, allowOverride: Boolean = true) {
        _di.addExtend(di = newDi, allowOverride = allowOverride)
    }

}

fun createDependenciesTree(platformConfiguration: PlatformConfiguration) {
    Inject.createDependenciesTree {
        bindSingleton { platformConfiguration }
        bindSingleton { buildHttpClient(true) }
        bindSingleton {
            AuthRepository(
                httpClient = instance(),
            )
        }
        bindSingleton {
            SettingsFactory(
                platformConfiguration = instance()
            ).createDefault()
        }
        bindSingleton {
            SettingsRepository(
                settings = instance()
            )
        }
    }
}