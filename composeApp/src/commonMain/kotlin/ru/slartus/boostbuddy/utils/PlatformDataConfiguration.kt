package ru.slartus.boostbuddy.utils

import org.kodein.di.bindSingleton
import org.kodein.di.instance
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.ktor.buildHttpClient
import ru.slartus.boostbuddy.data.repositories.AuthRepository
import ru.slartus.boostbuddy.data.repositories.BlogRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.SubscribesRepository
import ru.slartus.boostbuddy.data.settings.SettingsFactory

object PlatformDataConfiguration {
    fun createDependenciesTree(platformConfiguration: PlatformConfiguration) {
        Inject.createDependenciesTree {
            bindSingleton { GlobalExceptionHandlersChain() }
            bindSingleton { platformConfiguration }
            bindSingleton { buildHttpClient(true) }
            bindSingleton { AuthRepository(httpClient = instance()) }
            bindSingleton { SettingsFactory(platformConfiguration = instance()).createDefault() }
            bindSingleton { SettingsRepository(settings = instance()) }
            bindSingleton { SubscribesRepository(httpClient = instance()) }
            bindSingleton { BlogRepository(httpClient = instance()) }
        }
    }
}