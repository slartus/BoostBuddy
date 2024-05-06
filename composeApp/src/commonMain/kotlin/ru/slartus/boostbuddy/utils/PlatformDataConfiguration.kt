package ru.slartus.boostbuddy.utils

import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.ktor.buildBoostyHttpClient
import ru.slartus.boostbuddy.data.ktor.buildGithubHttpClient
import ru.slartus.boostbuddy.data.repositories.BlogRepository
import ru.slartus.boostbuddy.data.repositories.BoostyApi
import ru.slartus.boostbuddy.data.repositories.GithubRepository
import ru.slartus.boostbuddy.data.repositories.PostRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.SubscribesRepository
import ru.slartus.boostbuddy.data.repositories.VideoRepository
import ru.slartus.boostbuddy.data.repositories.comments.CommentsRepository
import ru.slartus.boostbuddy.data.settings.SettingsFactory

object PlatformDataConfiguration {
    private const val TAG_HTTP_CLIENT_BOOSTY = "boosty"
    private const val TAG_HTTP_CLIENT_GITHUB = "github"
    fun createDependenciesTree(platformConfiguration: PlatformConfiguration) {
        Inject.createDependenciesTree {
            bindSingleton { GlobalExceptionHandlersChain() }
            bindSingleton { platformConfiguration }
            bindSingleton { Permissions(platformConfiguration = instance()) }
            bindSingleton { SettingsRepository(settings = instance()) }
            bindSingleton { SettingsFactory(platformConfiguration = instance()).createDefault() }
            githubDependencies(platformConfiguration.isDebug)
            boostyDependencies(platformConfiguration.isDebug)
        }
    }

    private fun DI.MainBuilder.githubDependencies(isDebug: Boolean) {
        bindSingleton(TAG_HTTP_CLIENT_GITHUB) { buildGithubHttpClient(isDebug) }
        bindSingleton { GithubRepository(httpClient = instance(TAG_HTTP_CLIENT_GITHUB)) }
    }

    private fun DI.MainBuilder.boostyDependencies(isDebug: Boolean) {
        bindSingleton(TAG_HTTP_CLIENT_BOOSTY) {
            buildBoostyHttpClient(
                isDebug = isDebug,
                settingsRepository = instance()
            )
        }
        bindSingleton { BoostyApi(httpClient = instance(TAG_HTTP_CLIENT_BOOSTY)) }
        bindSingleton { SubscribesRepository(boostyApi = instance()) }
        bindSingleton { BlogRepository(boostyApi = instance()) }
        bindSingleton { CommentsRepository(boostyApi = instance()) }
        bindSingleton { PostRepository(boostyApi = instance()) }
        bindSingleton { VideoRepository(boostyApi = instance()) }
    }
}