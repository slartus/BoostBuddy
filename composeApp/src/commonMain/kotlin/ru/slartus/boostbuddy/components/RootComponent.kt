package ru.slartus.boostbuddy.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popTo
import com.arkivanov.decompose.router.stack.popWhile
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.components.auth.AuthComponent
import ru.slartus.boostbuddy.components.auth.AuthComponentImpl
import ru.slartus.boostbuddy.components.blog.BlogComponent
import ru.slartus.boostbuddy.components.blog.BlogComponentImpl
import ru.slartus.boostbuddy.components.subscribes.SubscribesComponent
import ru.slartus.boostbuddy.components.subscribes.SubscribesComponentImpl
import ru.slartus.boostbuddy.components.video.VideoComponent
import ru.slartus.boostbuddy.components.video.VideoComponentImpl
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.Blog
import ru.slartus.boostbuddy.data.repositories.GithubRepository
import ru.slartus.boostbuddy.data.repositories.ReleaseInfo
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.PostData
import ru.slartus.boostbuddy.utils.Permission
import ru.slartus.boostbuddy.utils.Permissions
import ru.slartus.boostbuddy.utils.Platform
import ru.slartus.boostbuddy.utils.PlatformConfiguration
import ru.slartus.boostbuddy.utils.VersionsComparer.greaterThan

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>
    val dialogSlot: Value<ChildSlot<*, DialogChild>>
    val viewStates: Value<RootViewState>

    // It's possible to pop multiple screens at a time on iOS
    fun onBackClicked(toIndex: Int)
    fun showAuthorizeComponent()

    fun onDialogVersionDismissed()
    fun onDialogVersionAcceptClicked(child: DialogChild.NewVersion)
    fun onDialogVersionCancelClicked()
    fun onDialogErrorDismissed()
    fun onErrorReceived(ex: Throwable)

    // Defines all possible child components
    sealed class Child {
        class AuthChild(val component: AuthComponent) : Child()
        class SubscribesChild(val component: SubscribesComponent) : Child()
        class BlogChild(val component: BlogComponent) : Child()
        class VideoChild(val component: VideoComponent) : Child()
    }

    sealed class DialogChild {
        data class NewVersion(val version: String, val info: String, val releaseInfo: ReleaseInfo) :
            DialogChild()

        data class Error(val message: String) : DialogChild()
    }
}

data class RootViewState(
    val darkMode: Boolean?
)

class RootComponentImpl(
    componentContext: ComponentContext,
) : BaseComponent<RootViewState>(componentContext, RootViewState(darkMode = null)), RootComponent {
    private val navigation = StackNavigation<Config>()
    private val dialogNavigation = SlotNavigation<DialogConfig>()
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val githubRepository by Inject.lazy<GithubRepository>()
    private val platformConfiguration by Inject.lazy<PlatformConfiguration>()
    private val permissions by Inject.lazy<Permissions>()

    override val dialogSlot: Value<ChildSlot<*, RootComponent.DialogChild>> =
        childSlot(
            source = dialogNavigation,
            serializer = DialogConfig.serializer(),
            handleBackButton = true,
            childFactory = ::dialogChild
        )

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Subscribes,
            handleBackButton = true,
            childFactory = ::child,
        )

    init {
        subscribeSettings()
        fetchLastReleaseInfo()
    }

    private fun subscribeSettings() {
        scope.launch {
            settingsRepository.darkModeFlow.collect {
                viewState = viewState.copy(darkMode = it)
            }
        }
    }

    private fun fetchLastReleaseInfo() {
        scope.launch {
            runCatching {
                val lastReleaseInfo =
                    githubRepository.getLastReleaseInfo().getOrNull() ?: return@launch
                val lastReleaseVersion = lastReleaseInfo.version

                if (!lastReleaseVersion.greaterThan(platformConfiguration.appVersion)) return@launch

                dialogNavigation.activate(
                    DialogConfig.NewVersion(releaseInfo = lastReleaseInfo)
                )
            }
        }
    }

    private fun downloadAndInstallNewVersion(releaseInfo: ReleaseInfo) {
        scope.launch {
            val url = when (platformConfiguration.platform) {
                Platform.Android,
                Platform.AndroidTV -> releaseInfo.androidDownloadUrl

                Platform.iOS -> null
            } ?: return@launch

            val path = githubRepository.downloadFile(url).getOrThrow()
            platformConfiguration.installApp(path)
        }
    }

    override fun showAuthorizeComponent() {
        navigation.push(Config.Auth)
    }

    private fun child(config: Config, componentContext: ComponentContext): RootComponent.Child =
        when (config) {
            is Config.Auth -> RootComponent.Child.AuthChild(authComponent(componentContext))
            is Config.Subscribes -> RootComponent.Child.SubscribesChild(
                subscribesComponent(
                    componentContext
                )
            )

            is Config.BlogConfig -> RootComponent.Child.BlogChild(
                blogComponent(
                    componentContext,
                    config
                )
            )

            is Config.VideoConfig -> RootComponent.Child.VideoChild(
                videoComponent(
                    componentContext,
                    config
                )
            )
        }

    private fun dialogChild(
        config: DialogConfig,
        componentContext: ComponentContext
    ): RootComponent.DialogChild =
        when (config) {
            is DialogConfig.NewVersion -> RootComponent.DialogChild.NewVersion(
                config.version,
                config.info,
                config.releaseInfo
            )

            is DialogConfig.Error -> RootComponent.DialogChild.Error(config.message)
        }

    private fun authComponent(componentContext: ComponentContext): AuthComponent =
        AuthComponentImpl(
            componentContext = componentContext,
            onLogined = {
                navigation.popWhile { it == Config.Auth }
            },
        )

    private fun subscribesComponent(componentContext: ComponentContext): SubscribesComponent =
        SubscribesComponentImpl(
            componentContext = componentContext,
            onItemSelected = {
                navigation.push(Config.BlogConfig(blog = it.blog))
            },
            onBackClicked = {
                navigation.pop()
            }
        )

    private fun blogComponent(
        componentContext: ComponentContext,
        config: Config.BlogConfig
    ): BlogComponent =
        BlogComponentImpl(
            componentContext = componentContext,
            blog = config.blog,
            onItemSelected = { postData, playerUrl ->
                navigation.push(Config.VideoConfig(postData = postData, playerUrl = playerUrl))
            },
            onBackClicked = {
                navigation.popWhile { it == config }
            }
        )

    private fun videoComponent(
        componentContext: ComponentContext,
        config: Config.VideoConfig
    ): VideoComponent =
        VideoComponentImpl(
            componentContext = componentContext,
            postData = config.postData,
            playerUrl = config.playerUrl,
            onStopClicked = { navigation.popWhile { it == config } }
        )

    override fun onBackClicked(toIndex: Int) {
        navigation.popTo(index = toIndex)
    }

    override fun onDialogVersionDismissed() {
        dialogNavigation.dismiss()
    }

    override fun onDialogVersionAcceptClicked(child: RootComponent.DialogChild.NewVersion) {
        dialogNavigation.dismiss()
        scope.launch {
            if (!permissions.isPermissionGranted(Permission.InstallApplication)) {
                runCatching {
                    permissions.providePermission(Permission.InstallApplication)
                    downloadAndInstallNewVersion(child.releaseInfo)
                }.onFailure {
                    dialogNavigation.activate(
                        DialogConfig.NewVersion(
                            releaseInfo = child.releaseInfo
                        )
                    )
                }
            } else {
                downloadAndInstallNewVersion(child.releaseInfo)
            }
        }
    }

    override fun onDialogVersionCancelClicked() {
        dialogNavigation.dismiss()
    }

    override fun onDialogErrorDismissed() {
        dialogNavigation.dismiss()
    }

    override fun onErrorReceived(ex: Throwable) {
        dialogNavigation.activate(
            DialogConfig.Error(
                message = ex.message ?: ex.toString()
            )
        )
    }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Auth : Config

        @Serializable
        data object Subscribes : Config

        @Serializable
        data class BlogConfig(val blog: Blog) : Config

        @Serializable
        data class VideoConfig(val postData: PostData.OkVideo, val playerUrl: PlayerUrl) : Config
    }

    @Serializable
    private sealed interface DialogConfig {
        @Serializable
        data class NewVersion(val releaseInfo: ReleaseInfo) : DialogConfig {
            val version: String = releaseInfo.version
            val info: String = releaseInfo.info.orEmpty()
        }

        @Serializable
        data class Error(val message: String) : DialogConfig
    }
}