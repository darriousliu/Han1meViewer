package io.github.darriousliu.han1meviewer.ui.navigation.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.darriousliu.han1meviewer.core.network.CloudflareNavBridge
import io.github.darriousliu.han1meviewer.core.ui.component.LocalToaster
import io.github.darriousliu.han1meviewer.core.ui.component.showShort
import io.github.darriousliu.han1meviewer.core.navigation.AccountRoute
import io.github.darriousliu.han1meviewer.core.navigation.AvatarCropRoute
import io.github.darriousliu.han1meviewer.core.navigation.AvatarCropped
import io.github.darriousliu.han1meviewer.core.navigation.CloudflareRoute
import io.github.darriousliu.han1meviewer.core.navigation.CreatorCenterRoute
import io.github.darriousliu.han1meviewer.core.navigation.DailyCheckInRoute
import io.github.darriousliu.han1meviewer.core.navigation.DownloadRoute
import io.github.darriousliu.han1meviewer.core.navigation.DownloadSettingsRoute
import io.github.darriousliu.han1meviewer.core.navigation.GetchuPreviewDetailRoute
import io.github.darriousliu.han1meviewer.core.navigation.GetchuPreviewRoute
import io.github.darriousliu.han1meviewer.core.navigation.HKeyframeSettingsRoute
import io.github.darriousliu.han1meviewer.core.navigation.HKeyframesRoute
import io.github.darriousliu.han1meviewer.core.navigation.HanimeRoute
import io.github.darriousliu.han1meviewer.core.navigation.HomeRefreshRequested
import io.github.darriousliu.han1meviewer.core.navigation.HomeRoute
import io.github.darriousliu.han1meviewer.core.navigation.HomeSettingsRoute
import io.github.darriousliu.han1meviewer.core.navigation.LoginRoute
import io.github.darriousliu.han1meviewer.core.navigation.LoginSucceeded
import io.github.darriousliu.han1meviewer.core.navigation.ManualCookiesRoute
import io.github.darriousliu.han1meviewer.core.navigation.MpvPlayerSettingsRoute
import io.github.darriousliu.han1meviewer.core.navigation.MyFavVideoRoute
import io.github.darriousliu.han1meviewer.core.navigation.MyPlaylistRoute
import io.github.darriousliu.han1meviewer.core.navigation.MyWatchLaterRoute
import io.github.darriousliu.han1meviewer.core.navigation.NetworkSettingsRoute
import io.github.darriousliu.han1meviewer.core.navigation.PlayerSettingsRoute
import io.github.darriousliu.han1meviewer.core.navigation.PreviewCommentRoute
import io.github.darriousliu.han1meviewer.core.navigation.PreviewRoute
import io.github.darriousliu.han1meviewer.core.navigation.SearchRoute
import io.github.darriousliu.han1meviewer.core.navigation.SharedHKeyframesRoute
import io.github.darriousliu.han1meviewer.core.navigation.SubscriptionRoute
import io.github.darriousliu.han1meviewer.core.navigation.VideoRoute
import io.github.darriousliu.han1meviewer.core.navigation.WatchHistoryRoute
import io.github.darriousliu.han1meviewer.core.navigation.navigateSafely
import io.github.darriousliu.han1meviewer.core.navigation.popTo
import io.github.darriousliu.han1meviewer.ui.navigation.settings.DownloadSettingsRouteScreen
import io.github.darriousliu.han1meviewer.feature.settings.HKeyframeSettingsRouteScreen
import io.github.darriousliu.han1meviewer.feature.settings.HKeyframesRouteScreen
import io.github.darriousliu.han1meviewer.feature.settings.HKeyframesTopBarActions
import io.github.darriousliu.han1meviewer.ui.navigation.settings.HomeSettingsRouteScreen
import io.github.darriousliu.han1meviewer.feature.settings.MpvPlayerSettingsRouteScreen
import io.github.darriousliu.han1meviewer.ui.navigation.settings.NetworkSettingsRouteScreen
import io.github.darriousliu.han1meviewer.ui.navigation.settings.PlayerSettingsRouteScreen
import io.github.darriousliu.han1meviewer.feature.settings.SettingsScaffold
import io.github.darriousliu.han1meviewer.feature.settings.SharedHKeyframesRouteScreen
import io.github.darriousliu.han1meviewer.feature.account.AccountScreen
import io.github.darriousliu.han1meviewer.feature.account.AvatarCropScreen
import io.github.darriousliu.han1meviewer.feature.subscription.creatorcenter.CreatorCenterScreen
import io.github.darriousliu.han1meviewer.feature.account.web.CloudflareScreen
import io.github.darriousliu.han1meviewer.feature.subscription.creatorcenter.CreatorCenterViewModel
import io.github.darriousliu.han1meviewer.feature.account.UserAccountViewModel
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.serialization.json.Json
import io.github.darriousliu.han1meviewer.feature.history.WatchHistoryRouteScreen
import io.github.darriousliu.han1meviewer.feature.subscription.SubscriptionRouteScreen
import io.github.darriousliu.han1meviewer.feature.preview.GetchuPreviewDetailRouteScreen
import io.github.darriousliu.han1meviewer.feature.preview.GetchuPreviewRouteScreen
import io.github.darriousliu.han1meviewer.feature.preview.PreviewCommentRouteScreen
import io.github.darriousliu.han1meviewer.feature.preview.PreviewRouteScreen
import io.github.darriousliu.han1meviewer.feature.mylist.MyPlaylistRouteScreen
import io.github.darriousliu.han1meviewer.feature.mylist.WatchLaterRouteScreen
import io.github.darriousliu.han1meviewer.feature.mylist.FavVideoRouteScreen
import io.github.darriousliu.han1meviewer.feature.search.SearchRouteScreen
import org.koin.compose.viewmodel.koinViewModel
import io.github.darriousliu.han1meviewer.feature.account.ManualCookiesRouteScreen
import io.github.darriousliu.han1meviewer.feature.account.LoginRouteScreen
import io.github.darriousliu.han1meviewer.feature.main.LocalMainHostActions
import io.github.darriousliu.han1meviewer.feature.main.MainHostActions

@Composable
fun MainNavDisplay(
    backStack: NavBackStack<HanimeRoute>,
    isDrawerOpen: Boolean,
    onOpenDrawer: () -> Unit,
    hostActions: MainHostActions,
) {
    val onBack: () -> Unit = { backStack.removeLastOrNull() }
    val onNavigateToVideo: (String) -> Unit = { code -> backStack.navigateSafely(VideoRoute(code)) }
    val onNavigateToLocalVideo: (String, String?) -> Unit =
        { code, uri -> backStack.navigateSafely(VideoRoute(code, uri)) }

    CompositionLocalProvider(LocalMainHostActions provides hostActions) {
        MainNavDisplayContent(
            backStack = backStack,
            isDrawerOpen = isDrawerOpen,
            onOpenDrawer = onOpenDrawer,
            hostActions = hostActions,
            onBack = onBack,
            onNavigateToVideo = onNavigateToVideo,
            onNavigateToLocalVideo = onNavigateToLocalVideo,
        )
    }
}

@Composable
private fun MainNavDisplayContent(
    backStack: NavBackStack<HanimeRoute>,
    isDrawerOpen: Boolean,
    onOpenDrawer: () -> Unit,
    hostActions: MainHostActions,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToLocalVideo: (String, String?) -> Unit,
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        // ⚠️ 显式传 entryDecorators 会**整个替换**默认值：
        //  - saveable 那个漏了，全 App 的 rememberSaveable 会静默失效
        //  - viewModelStore 那个漏了，entry-scoped 的 viewModel() 会退化成
        //    Activity scope（Getchu 列表/详情、收藏/稍后看会互相串数据）
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            rememberResultEventBusNavEntryDecorator(),
        ),
        // 动画参数为既定规格；predictivePopTransitionSpec 镜像 pop 保证观感一致。
        transitionSpec = {
            // 新页面：从右侧滑入 + 淡入；旧页面：向左偏移 1/3 + 缩小 + 淡出
            (slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(450, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(450))) togetherWith (slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                targetOffset = { it / 3 },
                animationSpec = tween(450, easing = FastOutSlowInEasing)
            ) + scaleOut(targetScale = 0.9f) + fadeOut(animationSpec = tween(300)))
        },
        popTransitionSpec = {
            // 返回：新页面从左侧 1/3 处滑入并由 0.9 放大恢复；旧页面向右滑出 + 淡出
            (slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                initialOffset = { it / 3 },
                animationSpec = tween(450, easing = FastOutSlowInEasing)
            ) + scaleIn(initialScale = 0.9f) + fadeIn(animationSpec = tween(450))) togetherWith
                    (slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(450, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(300)))
        },
        predictivePopTransitionSpec = { _ ->
            (slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                initialOffset = { it / 3 },
                animationSpec = tween(450, easing = FastOutSlowInEasing)
            ) + scaleIn(initialScale = 0.9f) + fadeIn(animationSpec = tween(450))) togetherWith
                    (slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(450, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(300)))
        },
        entryProvider = entryProvider {
            entry<HomeRoute> {
                HomeRouteScreen(
                    isDrawerOpen = isDrawerOpen,
                    onOpenDrawer = onOpenDrawer,
                    onNavigateToPreview = { backStack.navigateSafely(PreviewRoute) },
                    onNavigateToSearch = { query -> backStack.navigateSafely(SearchRoute(query = query)) },
                    onNavigateToSearchAdvanced = { params ->
                        backStack.navigateSafely(
                            SearchRoute(advancedSearchJson = Json.encodeToString(params))
                        )
                    },
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
            entry<WatchHistoryRoute> {
                WatchHistoryRouteScreen(
                    onBack = onBack,
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
            entry<MyFavVideoRoute> {
                FavVideoRouteScreen(
                    onBack = onBack,
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
            entry<MyWatchLaterRoute> {
                WatchLaterRouteScreen(
                    onBack = onBack,
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
            entry<MyPlaylistRoute> {
                MyPlaylistRouteScreen(
                    onBack = onBack,
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
            entry<SubscriptionRoute> {
                SubscriptionRouteScreen(
                    onBack = onBack,
                    onNavigateToSearch = { query -> backStack.navigateSafely(SearchRoute(query = query)) },
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
            entry<DailyCheckInRoute> {
                DailyCheckInRouteScreen(
                    onBack = onBack,
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
            entry<DownloadRoute> {
                DownloadRouteScreen(
                    onBack = onBack,
                    onNavigateToVideo = onNavigateToVideo,
                    onNavigateToLocalVideo = onNavigateToLocalVideo,
                )
            }
            entry<CreatorCenterRoute> {
                val creatorViewModel: CreatorCenterViewModel = koinViewModel()
                CreatorCenterScreen(
                    viewModel = creatorViewModel,
                    onBack = onBack,
                    onOpenUploadedVideo = { item -> onNavigateToVideo(item.videoCode) },
                    onOpenUploadingVideo = { item -> onNavigateToLocalVideo("-1", item.remoteVideoUrl) },
                )
            }
            entry<AccountRoute> {
                val toaster = LocalToaster.current
                val resultBus = LocalResultEventBus.current
                val accountViewModel: UserAccountViewModel = koinViewModel()
                ResultEffect<AvatarCropped> { result ->
                    accountViewModel.updateAvatar(result.jpeg, "avatar.jpg")
                }
                AccountScreen(
                    viewModel = accountViewModel,
                    onBack = onBack,
                    onOpenAvatarCrop = { file ->
                        backStack.navigateSafely(
                            AvatarCropRoute(Json.encodeToString(PlatformFile.serializer(), file))
                        )
                    },
                    onRefreshHome = { resultBus.sendResult(HomeRefreshRequested) },
                    onMessage = toaster::showShort,
                    onLogout = { hostActions.onLogout(closeCurrentPageOnConfirm = true) },
                )
            }
            entry<AvatarCropRoute> { route ->
                val resultBus = LocalResultEventBus.current
                val source = remember(route.sourceJson) {
                    runCatching {
                        Json.decodeFromString(PlatformFile.serializer(), route.sourceJson)
                    }.getOrNull()
                }
                // 反序列化不出来只可能是路由参数被外部改坏，直接退回去
                if (source == null) {
                    LaunchedEffect(Unit) { onBack() }
                } else {
                    AvatarCropScreen(
                        source = source,
                        onBack = onBack,
                        onConfirm = { jpegBytes ->
                            resultBus.sendResult(AvatarCropped(jpegBytes))
                            onBack()
                        },
                    )
                }
            }
            entry<HomeSettingsRoute> {
                SettingsScaffold(
                    backStack = backStack,
                    fallbackDestination = HomeRoute,
                ) {
                    HomeSettingsRouteScreen(
                        onNavigateToPlayerSettings = { backStack.navigateSafely(PlayerSettingsRoute) },
                        onNavigateToHKeyframeSettings = { backStack.navigateSafely(HKeyframeSettingsRoute) },
                        onNavigateToDownloadSettings = { backStack.navigateSafely(DownloadSettingsRoute) },
                        onNavigateToNetworkSettings = { backStack.navigateSafely(NetworkSettingsRoute) },
                    )
                }
            }
            entry<PlayerSettingsRoute> {
                SettingsScaffold(
                    backStack = backStack,
                    fallbackDestination = HomeSettingsRoute,
                ) {
                    PlayerSettingsRouteScreen(
                        onNavigateToMpvSettings = { backStack.navigateSafely(MpvPlayerSettingsRoute) },
                    )
                }
            }
            entry<NetworkSettingsRoute> {
                SettingsScaffold(
                    backStack = backStack,
                    fallbackDestination = HomeSettingsRoute,
                ) {
                    NetworkSettingsRouteScreen()
                }
            }
            entry<DownloadSettingsRoute> {
                SettingsScaffold(
                    backStack = backStack,
                    fallbackDestination = HomeSettingsRoute,
                ) {
                    DownloadSettingsRouteScreen()
                }
            }
            entry<MpvPlayerSettingsRoute> {
                SettingsScaffold(
                    backStack = backStack,
                    fallbackDestination = PlayerSettingsRoute,
                ) {
                    MpvPlayerSettingsRouteScreen()
                }
            }
            entry<HKeyframesRoute> {
                var showImportDialog by remember { mutableStateOf(false) }
                SettingsScaffold(
                    backStack = backStack,
                    fallbackDestination = HKeyframeSettingsRoute,
                    actions = {
                        HKeyframesTopBarActions(onImportClick = { showImportDialog = true })
                    },
                ) {
                    HKeyframesRouteScreen(
                        onOpenVideo = onNavigateToVideo,
                        showImportDialog = showImportDialog,
                        onImportDialogDismiss = { showImportDialog = false },
                    )
                }
            }
            entry<SharedHKeyframesRoute> {
                SettingsScaffold(
                    backStack = backStack,
                    fallbackDestination = HKeyframeSettingsRoute,
                ) {
                    SharedHKeyframesRouteScreen(
                        onOpenVideo = onNavigateToVideo,
                    )
                }
            }
            entry<HKeyframeSettingsRoute> {
                SettingsScaffold(
                    backStack = backStack,
                    fallbackDestination = HomeSettingsRoute,
                ) {
                    HKeyframeSettingsRouteScreen(
                        onNavigateToHKeyframes = { backStack.navigateSafely(HKeyframesRoute) },
                        onNavigateToSharedHKeyframes = { backStack.navigateSafely(SharedHKeyframesRoute) },
                    )
                }
            }
            entry<SearchRoute> { route ->
                SearchRouteScreen(
                    route = route,
                    onBack = onBack,
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
            entry<PreviewRoute> {
                PreviewRouteScreen(
                    onBack = onBack,
                    onNavigateToGetchuPreview = {
                        backStack.navigateSafely(GetchuPreviewRoute)
                    },
                    onNavigateToPreviewComment = { date, dateCode ->
                        backStack.navigateSafely(PreviewCommentRoute(date, dateCode))
                    },
                    onNavigateToVideo = onNavigateToVideo,
                )
            }
            entry<GetchuPreviewRoute> {
                GetchuPreviewRouteScreen(
                    onBack = onBack,
                    onNavigateToDetail = { id -> backStack.navigateSafely(GetchuPreviewDetailRoute(id)) },
                )
            }
            entry<GetchuPreviewDetailRoute> { route ->
                GetchuPreviewDetailRouteScreen(
                    route = route,
                    onBack = onBack,
                    onNavigateToDetail = { id -> backStack.navigateSafely(GetchuPreviewDetailRoute(id)) },
                    onNavigateToVideoUrl = { url -> backStack.navigateSafely(VideoRoute("-1", url)) },
                )
            }
            entry<PreviewCommentRoute> { route ->
                PreviewCommentRouteScreen(
                    route = route,
                    onBack = onBack,
                )
            }
            entry<VideoRoute> { route ->
                VideoRouteScreen(route = route)
            }

            entry<LoginRoute> {
                val resultBus = LocalResultEventBus.current
                LoginRouteScreen(
                    onBack = onBack,
                    onOpenManualCookies = { backStack.navigateSafely(ManualCookiesRoute) },
                    onLoginFinished = {
                        resultBus.sendResult(LoginSucceeded)
                        backStack.removeLastOrNull()
                    },
                )
            }

            entry<CloudflareRoute> { route ->
                // 死锁修复的关键：无论怎么离开这个页面（过完自动 pop / 用户按返回 / 被销毁），
                // 等待中的网络请求都必须被放行，否则全局 mutex 会一直卡住后续撞盾请求
                DisposableEffect(Unit) {
                    onDispose {
                        CloudflareNavBridge.pending.value?.done?.complete(Unit)
                    }
                }
                CloudflareScreen(
                    url = route.url,
                    onSolved = onBack,
                    onClose = onBack,
                )
            }

            entry<ManualCookiesRoute> {
                val resultBus = LocalResultEventBus.current
                ManualCookiesRouteScreen(
                    onBack = onBack,
                    onLoginFinished = {
                        resultBus.sendResult(LoginSucceeded)
                        // 把手动输入页和登录页一起退掉
                        backStack.popTo(LoginRoute, inclusive = true)
                    },
                )
            }
        },
    )
}
