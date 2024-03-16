package ru.slartus.boostbuddy.utils

import org.kodein.di.bindSingleton
import org.kodein.di.instance
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.ktor.buildHttpClient
import ru.slartus.boostbuddy.data.repositories.AuthRepository
import ru.slartus.boostbuddy.data.repositories.BlogRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.SubscribesRepository
import ru.slartus.boostbuddy.data.repositories.GithubRepository
import ru.slartus.boostbuddy.data.settings.SettingsFactory

object PlatformDataConfiguration {
    private const val TAG_HTTP_CLIENT_BOOSTY="boosty"
    private const val TAG_HTTP_CLIENT_GITHUB="github"
    fun createDependenciesTree(platformConfiguration: PlatformConfiguration) {
        Inject.createDependenciesTree {
            bindSingleton { GlobalExceptionHandlersChain() }
            bindSingleton { platformConfiguration }
            bindSingleton { Permissions(platformConfiguration = instance()) }
            bindSingleton(TAG_HTTP_CLIENT_BOOSTY) { buildHttpClient(platformConfiguration.isDebug) }
            bindSingleton(TAG_HTTP_CLIENT_GITHUB) { buildHttpClient(platformConfiguration.isDebug) }
            bindSingleton { AuthRepository(httpClient = instance(TAG_HTTP_CLIENT_BOOSTY)) }
            bindSingleton { SettingsFactory(platformConfiguration = instance()).createDefault() }
            bindSingleton { SettingsRepository(settings = instance()) }
            bindSingleton { SubscribesRepository(httpClient = instance(TAG_HTTP_CLIENT_BOOSTY)) }
            bindSingleton { BlogRepository(httpClient = instance(TAG_HTTP_CLIENT_BOOSTY)) }
            bindSingleton { GithubRepository(httpClient = instance(TAG_HTTP_CLIENT_GITHUB)) }
        }
    }
}