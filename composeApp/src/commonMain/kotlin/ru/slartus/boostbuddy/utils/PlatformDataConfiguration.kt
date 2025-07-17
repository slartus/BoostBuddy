package ru.slartus.boostbuddy.utils

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import org.kodein.di.new
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.ktor.buildBoostyHttpClient
import ru.slartus.boostbuddy.data.ktor.buildGithubHttpClient
import ru.slartus.boostbuddy.data.repositories.AppSettings
import ru.slartus.boostbuddy.data.repositories.BlogRepository
import ru.slartus.boostbuddy.data.repositories.BoostyApi
import ru.slartus.boostbuddy.data.repositories.EventsRepository
import ru.slartus.boostbuddy.data.repositories.FeedRepository
import ru.slartus.boostbuddy.data.repositories.GithubRepository
import ru.slartus.boostbuddy.data.repositories.PostRepository
import ru.slartus.boostbuddy.data.repositories.ProfileRepository
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.SubscribesRepository
import ru.slartus.boostbuddy.data.repositories.VideoRepository
import ru.slartus.boostbuddy.data.repositories.comments.CommentsRepository
import ru.slartus.boostbuddy.data.settings.SettingsFactory
import ru.slartus.boostbuddy.navigation.NavigationRouterImpl

object PlatformDataConfiguration {
    private const val TAG_HTTP_CLIENT_BOOSTY = "boosty"
    private const val TAG_HTTP_CLIENT_GITHUB = "github"
    fun createDependenciesTree(platformConfiguration: PlatformConfiguration) {
        Inject.createDependenciesTree {
            bindSingleton {
                CoroutineScope(
                    SupervisorJob() + Dispatchers.IO +
                            CoroutineExceptionHandler { _, exception ->
                                Napier.e("Main scope error", exception)
                            }
                )
            }
            bindSingleton { GlobalExceptionHandlersChain() }
            bindSingleton { platformConfiguration }
            bindSingleton { NavigationRouterImpl() }
            bindSingleton { Permissions(platformConfiguration = instance()) }
            bindSingleton { SettingsFactory(platformConfiguration = instance()).createDefault() }
            bindProvider { new(::SettingsRepository) }
        }
        val appSettings = getAppSettings()
        val bufferLoggingTracker = addBufferLoggingTracker(appSettings.debugLog)
        Inject.addConfig {
            bindSingleton { bufferLoggingTracker }
            githubDependencies(appSettings.debugLog)
            boostyDependencies(appSettings.debugLog)
        }
    }

    private fun getAppSettings(): AppSettings {
        val settingsRepository = Inject.instance<SettingsRepository>()
        return runBlocking { settingsRepository.getSettings() }
    }

    private fun addBufferLoggingTracker(debugLog: Boolean): BufferLoggingTracker {
        val bufferLoggingTracker = BufferLoggingTracker(debugLog)
        Napier.base(bufferLoggingTracker)
        return bufferLoggingTracker
    }

    private fun DI.MainBuilder.githubDependencies(debugLog: Boolean) {
        bindSingleton(TAG_HTTP_CLIENT_GITHUB) { buildGithubHttpClient(debugLog) }
        bindSingleton { GithubRepository(httpClient = instance(TAG_HTTP_CLIENT_GITHUB)) }
    }

    private fun DI.MainBuilder.boostyDependencies(debugLog: Boolean) {
        bindSingleton(TAG_HTTP_CLIENT_BOOSTY) {
            buildBoostyHttpClient(
                debugLog = debugLog,
                settingsRepository = instance()
            )
        }
        bindSingleton { BoostyApi(httpClient = instance(TAG_HTTP_CLIENT_BOOSTY)) }
        bindSingleton { SubscribesRepository(boostyApi = instance()) }
        bindSingleton { BlogRepository(boostyApi = instance()) }
        bindSingleton { CommentsRepository(boostyApi = instance()) }
        bindSingleton { PostRepository(boostyApi = instance()) }
        bindSingleton { VideoRepository(boostyApi = instance()) }
        bindSingleton { ProfileRepository(boostyApi = instance()) }
        bindSingleton { EventsRepository(boostyApi = instance()) }
        bindSingleton { FeedRepository(boostyApi = instance()) }
    }
}