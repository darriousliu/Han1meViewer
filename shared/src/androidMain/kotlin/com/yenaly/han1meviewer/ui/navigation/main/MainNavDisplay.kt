package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.yenaly.han1meviewer.logic.network.CloudflareNavBridge
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.component.LocalToaster
import com.yenaly.han1meviewer.util.copyTextToClipboard
import com.yenaly.han1meviewer.ui.component.showShort
import com.yenaly.han1meviewer.ui.navigation.AccountRoute
import com.yenaly.han1meviewer.ui.navigation.AvatarCropRoute
import com.yenaly.han1meviewer.ui.navigation.AvatarCropped
import com.yenaly.han1meviewer.ui.navigation.CloudflareRoute
import com.yenaly.han1meviewer.ui.navigation.CreatorCenterRoute
import com.yenaly.han1meviewer.ui.navigation.DailyCheckInRoute
import com.yenaly.han1meviewer.ui.navigation.DownloadRoute
import com.yenaly.han1meviewer.ui.navigation.DownloadSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.GetchuPreviewDetailRoute
import com.yenaly.han1meviewer.ui.navigation.GetchuPreviewRoute
import com.yenaly.han1meviewer.ui.navigation.HKeyframeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.HKeyframesRoute
import com.yenaly.han1meviewer.ui.navigation.HanimeRoute
import com.yenaly.han1meviewer.ui.navigation.HomeRoute
import com.yenaly.han1meviewer.ui.navigation.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.LoginRoute
import com.yenaly.han1meviewer.ui.navigation.LoginSucceeded
import com.yenaly.han1meviewer.ui.navigation.ManualCookiesRoute
import com.yenaly.han1meviewer.ui.navigation.MpvPlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.MyFavVideoRoute
import com.yenaly.han1meviewer.ui.navigation.MyPlaylistRoute
import com.yenaly.han1meviewer.ui.navigation.MyWatchLaterRoute
import com.yenaly.han1meviewer.ui.navigation.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.PlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.PreviewCommentRoute
import com.yenaly.han1meviewer.ui.navigation.PreviewRoute
import com.yenaly.han1meviewer.ui.navigation.SearchRoute
import com.yenaly.han1meviewer.ui.navigation.SharedHKeyframesRoute
import com.yenaly.han1meviewer.ui.navigation.SubscriptionRoute
import com.yenaly.han1meviewer.ui.navigation.VideoRoute
import com.yenaly.han1meviewer.ui.navigation.WatchHistoryRoute
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.navigation.popTo
import com.yenaly.han1meviewer.ui.navigation.settings.DownloadSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframeSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframesRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframesTopBarActions
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.MpvPlayerSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRouteScreen
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsScaffold
import com.yenaly.han1meviewer.ui.navigation.settings.SharedHKeyframesRouteScreen
import com.yenaly.han1meviewer.ui.screen.account.AccountScreen
import com.yenaly.han1meviewer.ui.screen.account.AvatarCropScreen
import com.yenaly.han1meviewer.ui.screen.home.CreatorCenterScreen
import com.yenaly.han1meviewer.ui.screen.web.CloudflareScreen
import com.yenaly.han1meviewer.ui.viewmodel.CreatorCenterViewModel
import com.yenaly.han1meviewer.ui.viewmodel.UserAccountViewModel
import com.yenaly.han1meviewer.util.showShortToast
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.serialization.json.Json

@Composable
fun MainNavDisplay(
    activity: MainActivity,
    backStack: NavBackStack<HanimeRoute>,
    isDrawerOpen: Boolean,
    onOpenDrawer: () -> Unit,
) {
    val onBack: () -> Unit = { backStack.removeLastOrNull() }
    val onNavigateToVideo: (String) -> Unit = { code -> backStack.navigateSafely(VideoRoute(code)) }
    val onNavigateToLocalVideo: (String, String?) -> Unit =
        { code, uri -> backStack.navigateSafely(VideoRoute(code, uri)) }

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
        // nav2 是 enter/exit/popEnter/popExit 四条各返回单侧动画，
        // nav3 是 transitionSpec/popTransitionSpec 两条各返回 ContentTransform（enter togetherWith exit），
        // 外加一条 predictivePopTransitionSpec（nav2 没有，这里镜像 pop 保证观感一致）。
        // 内容逐字照搬——nav3 的 receiver 就是 AnimatedContentTransitionScope，
        // slideIntoContainer / slideOutOfContainer 原样可用。
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
                // 登录成功后刷新首页。原来是 LoginActivity 的 setResult(RESULT_OK)
                // → loginDataLauncher → getHomePage()，Step 17 退化成回调透传，
                // 现在走 nav3 的结果总线：登录页 pop 回来时这里收到
                ResultEffect<LoginSucceeded> { activity.viewModel.getHomePage() }
                HomeRouteScreen(
                    activity = activity,
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
                    onCopy = ::copyTextToClipboard,
                )
            }
            entry<SubscriptionRoute> {
                SubscriptionRouteScreen(
                    onBack = onBack,
                    onNavigateToSearch = { query -> backStack.navigateSafely(SearchRoute(query = query)) },
                    onNavigateToVideo = onNavigateToVideo,
                    onCopy = ::copyTextToClipboard,
                )
            }
            entry<DailyCheckInRoute> {
                DailyCheckInRouteScreen(
                    activity = activity,
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
                val creatorViewModel: CreatorCenterViewModel = viewModel()
                CreatorCenterScreen(
                    viewModel = creatorViewModel,
                    onBack = onBack,
                    onOpenUploadedVideo = { item -> onNavigateToVideo(item.videoCode) },
                    onOpenUploadingVideo = { item -> onNavigateToLocalVideo("-1", item.remoteVideoUrl) },
                )
            }
            entry<AccountRoute> {
                val toaster = LocalToaster.current
                val accountViewModel: UserAccountViewModel = viewModel()
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
                    onRefreshHome = { activity.viewModel.getHomePage() },
                    onMessage = toaster::showShort,
                    onLogout = { activity.showLogoutConfirmDialog(closeCurrentPageOnConfirm = true) },
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
                        activity = activity,
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
                    DownloadSettingsRouteScreen(activity = activity)
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
                        onCopy = ::copyTextToClipboard,
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
                    onCopy = ::copyTextToClipboard,
                )
            }
            entry<PreviewRoute> {
                PreviewRouteScreen(
                    activity = activity,
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
                    activity = activity,
                    route = route,
                    onBack = onBack,
                )
            }
            entry<VideoRoute> { route ->
                VideoRouteScreen(
                    activity = activity,
                    route = route,
                )
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
                        // 把手动输入页和登录页一起退掉（原来是两层 Activity 各自 finish）
                        backStack.popTo(LoginRoute, inclusive = true)
                    },
                )
            }
        },
    )
}
