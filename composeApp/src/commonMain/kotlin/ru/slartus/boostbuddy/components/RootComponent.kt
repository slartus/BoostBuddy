package ru.slartus.boostbuddy.components

import androidx.compose.runtime.Stable
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
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.slartus.boostbuddy.components.auth.AuthComponent
import ru.slartus.boostbuddy.components.auth.AuthComponentImpl
import ru.slartus.boostbuddy.components.blog.BlogComponent
import ru.slartus.boostbuddy.components.blog.BlogComponentImpl
import ru.slartus.boostbuddy.components.post.PostComponent
import ru.slartus.boostbuddy.components.post.PostComponentImpl
import ru.slartus.boostbuddy.components.settings.SettingsComponent
import ru.slartus.boostbuddy.components.settings.SettingsComponentImpl
import ru.slartus.boostbuddy.components.subscribes.SubscribesComponent
import ru.slartus.boostbuddy.components.subscribes.SubscribesComponentImpl
import ru.slartus.boostbuddy.components.video.VideoComponent
import ru.slartus.boostbuddy.components.video.VideoComponentImpl
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.AppSettings
import ru.slartus.boostbuddy.data.repositories.Blog
import ru.slartus.boostbuddy.data.repositories.GithubRepository
import ru.slartus.boostbuddy.data.repositories.ReleaseInfo
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.models.Content
import ru.slartus.boostbuddy.data.repositories.models.PlayerUrl
import ru.slartus.boostbuddy.data.repositories.models.Post
import ru.slartus.boostbuddy.utils.Permission
import ru.slartus.boostbuddy.utils.Permissions
import ru.slartus.boostbuddy.utils.Platform
import ru.slartus.boostbuddy.utils.PlatformConfiguration
import ru.slartus.boostbuddy.utils.VersionsComparer.greaterThan
import ru.slartus.boostbuddy.utils.VideoPlayer

@Stable
interface RootComponent : AppComponent<RootViewAction> {
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
    fun onDialogSettingsDismissed()

    // Defines all possible child components
    sealed class Child {
        class AuthChild(val component: AuthComponent) : Child()
        class SubscribesChild(val component: SubscribesComponent) : Child()
        class BlogChild(val component: BlogComponent) : Child()
        class VideoChild(val component: VideoComponent) : Child()
        class PostChild(val component: PostComponent) : Child()
    }

    sealed class DialogChild {
        data class NewVersion(val version: String, val info: String, val releaseInfo: ReleaseInfo) :
            DialogChild()

        data class Error(val message: String) : DialogChild()

        data class AppSettings(val component: SettingsComponent) : DialogChild()
    }
}

data class RootViewState(
    val appSettings: AppSettings
)

sealed class RootViewAction {
    data class ShowSnackBar(val message: String) : RootViewAction()
}

class RootComponentImpl(
    componentContext: ComponentContext,
) : BaseComponent<RootViewState, RootViewAction>(
    componentContext,
    RootViewState(appSettings = AppSettings.Default)
),
    RootComponent {
    private val navigation = StackNavigation<Config>()
    private val dialogNavigation = SlotNavigation<DialogConfig>()
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val githubRepository by Inject.lazy<GithubRepository>()
    private val platformConfiguration by Inject.lazy<PlatformConfiguration>()
    private val permissions by Inject.lazy<Permissions>()

    override val dialogSlot: Value<ChildSlot<*, RootComponent.DialogChild>> =
        childSlot(
            key = "dialogSlot",
            source = dialogNavigation,
            serializer = DialogConfig.serializer(),
            handleBackButton = true,
            childFactory = ::dialogChild
        )

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            key = "DefaultChildStack",
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
            settingsRepository.appSettingsFlow.collect {
                viewState = viewState.copy(appSettings = it)
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

            viewAction = RootViewAction.ShowSnackBar("Загрузка файла началась")
            val path = githubRepository.downloadFile(url).getOrThrow()
            runCatching {
                platformConfiguration.installApp(path)
            }.onFailure {
                viewAction = RootViewAction.ShowSnackBar("Ошибка загрузки файла")
            }
        }
    }

    override fun showAuthorizeComponent() {
        navigation.replaceAll(Config.Auth)
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

            is Config.PostConfig -> RootComponent.Child.PostChild(
                postComponent(
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
            DialogConfig.AppSettings -> RootComponent.DialogChild.AppSettings(
                settingsComponent(
                    componentContext
                )
            )
        }

    private fun settingsComponent(componentContext: ComponentContext): SettingsComponent =
        SettingsComponentImpl(componentContext = componentContext)

    private fun authComponent(componentContext: ComponentContext): AuthComponent =
        AuthComponentImpl(
            componentContext = componentContext,
            onLogined = {
                navigation.replaceAll(Config.Subscribes)
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
            },
            onAppSettingsClicked = {
                dialogNavigation.activate(
                    DialogConfig.AppSettings
                )
            }
        )

    private fun blogComponent(
        componentContext: ComponentContext,
        config: Config.BlogConfig
    ): BlogComponent =
        BlogComponentImpl(
            componentContext = componentContext,
            blog = config.blog,
            onItemSelected = { postId, postData, playerUrl ->
                playVideo(
                    blogUrl = config.blog.blogUrl,
                    postId = postId,
                    postData = postData,
                    playerUrl = playerUrl
                )
            },
            onBackClicked = {
                navigation.popWhile { it == config }
            },
            onPostSelected = { blog, post ->
                navigation.push(Config.PostConfig(blogUrl = blog.blogUrl, post = post))
            }
        )

    private fun postComponent(
        componentContext: ComponentContext,
        config: Config.PostConfig
    ): PostComponent =
        PostComponentImpl(
            componentContext = componentContext,
            blogUrl = config.blogUrl,
            post = config.post,
            onBackClicked = { navigation.popWhile { it == config } },
            onItemSelected = { postId, postData, playerUrl ->
                playVideo(
                    blogUrl = config.blogUrl,
                    postId = postId,
                    postData = postData,
                    playerUrl = playerUrl
                )
            },
        )

    private fun playVideo(
        blogUrl: String,
        postId: String,
        postData: Content.OkVideo,
        playerUrl: PlayerUrl
    ) {
        scope.launch {
            val settings = settingsRepository.getSettings()
            if (settings.useSystemVideoPlayer) {
                val player = VideoPlayer()
                player.playUrl(
                    platformConfiguration = platformConfiguration,
                    title = postData.title,
                    url = playerUrl.url,
                    mimeType = "video/*",
                    posterUrl = postData.previewUrl
                )
            } else {
                navigation.push(
                    Config.VideoConfig(
                        blogUrl = blogUrl,
                        postId = postId,
                        postData = postData,
                        playerUrl = playerUrl
                    )
                )
            }
        }
    }

    private fun videoComponent(
        componentContext: ComponentContext,
        config: Config.VideoConfig
    ): VideoComponent =
        VideoComponentImpl(
            componentContext = componentContext,
            blogUrl = config.blogUrl,
            postId = config.postId,
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
        viewAction = RootViewAction.ShowSnackBar(ex.message ?: ex.toString())
    }

    override fun onDialogSettingsDismissed() {
        dialogNavigation.dismiss()
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
        data class VideoConfig(
            val blogUrl: String,
            val postId: String,
            val postData: Content.OkVideo,
            val playerUrl: PlayerUrl
        ) : Config

        @Serializable
        data class PostConfig(val blogUrl: String, val post: Post) : Config
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

        @Serializable
        data object AppSettings : DialogConfig
    }
}