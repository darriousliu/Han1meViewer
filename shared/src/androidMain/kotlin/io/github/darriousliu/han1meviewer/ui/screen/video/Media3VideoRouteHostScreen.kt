package io.github.darriousliu.han1meviewer.ui.screen.video

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import io.github.darriousliu.han1meviewer.PermissionRequester
import io.github.darriousliu.han1meviewer.R
import io.github.darriousliu.han1meviewer.core.common.HanimeConstants
import io.github.darriousliu.han1meviewer.core.common.ResolutionLinkMap
import io.github.darriousliu.han1meviewer.core.common.exception.ParseException
import io.github.darriousliu.han1meviewer.core.common.state.VideoLoadingState
import io.github.darriousliu.han1meviewer.core.common.util.copyToClipboard
import io.github.darriousliu.han1meviewer.core.common.util.loadBundledJson
import io.github.darriousliu.han1meviewer.core.common.util.localizedTextOrNull
import io.github.darriousliu.han1meviewer.core.firebase.FirebaseConstants
import io.github.darriousliu.han1meviewer.core.model.SearchOption
import io.github.darriousliu.han1meviewer.core.navigation.HomeRoute
import io.github.darriousliu.han1meviewer.core.navigation.VideoRoute
import io.github.darriousliu.han1meviewer.core.navigation.popTo
import io.github.darriousliu.han1meviewer.core.repository.DatabaseRepo
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.player_tips_not_wifi
import io.github.darriousliu.han1meviewer.core.resource.player_tips_not_wifi_cancel
import io.github.darriousliu.han1meviewer.core.resource.player_tips_not_wifi_confirm
import io.github.darriousliu.han1meviewer.core.resource.warning
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.storage.dao.CheckInRecordDatabase
import io.github.darriousliu.han1meviewer.core.storage.entity.HKeyframeEntity
import io.github.darriousliu.han1meviewer.core.storage.entity.WatchHistoryEntity
import io.github.darriousliu.han1meviewer.core.storage.getHanimeVideoLink
import io.github.darriousliu.han1meviewer.core.ui.component.ConfirmDialog
import io.github.darriousliu.han1meviewer.feature.comment.CommentViewModel
import io.github.darriousliu.han1meviewer.feature.video.DownloadPromptState
import io.github.darriousliu.han1meviewer.feature.video.VideoPageHost
import io.github.darriousliu.han1meviewer.feature.video.VideoRouteContent
import io.github.darriousliu.han1meviewer.feature.video.VideoViewModel
import io.github.darriousliu.han1meviewer.feature.video.player.HanimeVideoPlayer
import io.github.darriousliu.han1meviewer.feature.video.player.MobileDataWarningSession
import io.github.darriousliu.han1meviewer.feature.video.player.PlaybackVisual
import io.github.darriousliu.han1meviewer.feature.video.player.SuperResolutionController
import io.github.darriousliu.han1meviewer.feature.video.player.playbackVisualOf
import io.github.darriousliu.han1meviewer.feature.video.player.rememberAndroidPlayerEnvironment
import io.github.darriousliu.han1meviewer.feature.video.player.rememberDeviceMediaControls
import io.github.darriousliu.han1meviewer.feature.video.player.rememberVideoPlayerController
import io.github.darriousliu.han1meviewer.ui.activity.MainActivity
import io.github.darriousliu.han1meviewer.util.OrientationManager
import io.github.darriousliu.han1meviewer.util.browse
import io.github.darriousliu.han1meviewer.util.checkBadGuy
import io.github.darriousliu.han1meviewer.util.shareText
import io.github.darriousliu.han1meviewer.util.showShortToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Media3 + Compose 的视频页宿主。
 *
 * 和旧的 [VideoRouteHostScreen] **完全隔离**——旧路径一个字没动，
 * 在设置页把内核切回去就能回到它。所以这里刻意重复了 ViewModel/加载/actions 的接线，
 * 而不是把那 719 行抽成共用接口：改坏了只能整批回退的前提下，隔离比 DRY 值钱。
 *
 * ⚠️ 本文件**绝不能**调 `Jzvd.releaseAllVideos()` / `goOnPlayOnPause()` / `backPress()`——
 * 那三个是全局静态，两套播放器共用会互相踩。
 *
 * 与旧宿主的两处**有意的行为差异**（用户实机验证后拍板）：
 * 1. 播放器位置固定，滚动不折叠——纯 Compose 折叠观感差，且容器高度变化会经
 *    SurfaceHolder 改写 mpv 的 android-surface-size，缩小状态切全屏曾致画面残缺；
 * 2. 进入页面**不自动播放**：加载完成停在暂停态（首帧可见），由用户点播放。
 */
@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
fun Media3VideoRouteHostScreen(
    activity: MainActivity,
    route: VideoRoute,
) {
    val scope = rememberCoroutineScope()
    // 两个 VM 都是 NavEntry 作用域 + 构造注入：一个视频页一个实例，
    // 挂 Activity scope 会让栈里叠着的两个视频页拿到同一份实例（factory 被静默忽略）。
    val viewModel: VideoViewModel = viewModel(
        factory = VideoViewModel.factory(route.videoCode, route.localUri),
    )
    val commentViewModel: CommentViewModel = viewModel(
        factory = CommentViewModel.factory(route.videoCode),
    )
    val controller = rememberVideoPlayerController(route.videoCode to route.localUri)
    val videoState by viewModel.hanimeVideoStateFlow.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    var title by remember(route) { mutableStateOf("") }
    var checkedQuality by remember(route) { mutableStateOf<String?>(null) }
    var pendingDownloadPrompt by remember(route) { mutableStateOf<DownloadPromptState?>(null) }
    var genres by remember(Preferences.baseUrl) { mutableStateOf(emptyList<SearchOption>()) }
    /** 上次看到的进度（毫秒）；续播按钮读它。 */
    var savedProgressMs by remember(route) { mutableLongStateOf(0L) }
    /** 解析结果里没有可播放链接时置 false；播放钮据此转为「跳浏览器」。 */
    var hasPlayableSource by remember(route) { mutableStateOf(true) }
    var coverUrl by remember(route) { mutableStateOf<String?>(null) }
    var videoUrls by remember(route) { mutableStateOf<ResolutionLinkMap?>(null) }
    /** 正在播放的清晰度 key（与下载用的 [checkedQuality] 是两回事）。 */
    var playingQuality by remember(route) { mutableStateOf<String?>(null) }
    var showAddHKeyframeDialog by remember(route) { mutableStateOf<Pair<Long, String>?>(null) }
    var superResolutionIndex by remember(route) { mutableIntStateOf(0) }
    val hKeyframes by viewModel.hKeyframes.collectAsStateWithLifecycle()
    val hostUiState by viewModel.videoHostUiStateFlow.collectAsStateWithLifecycle()

    // ---- 全屏 / 方向 / PiP ----
    val visual = playbackVisualOf(controller)
    var isFullscreen by rememberSaveable(route.videoCode, route.localUri) { mutableStateOf(false) }
    val fullscreenEnterFraction = remember { Animatable(1f) }
    var playerBounds by remember { mutableStateOf<Rect?>(null) }
    var lastAutoFullscreenAt by remember { mutableLongStateOf(0L) }
    val deviceControls = rememberDeviceMediaControls()
    val environment = rememberAndroidPlayerEnvironment()
    /** 流量提醒确认前挂起的 (url, 起播位置)；确认后播、取消则点播放钮再弹。 */
    var pendingMeteredPlay by remember(route) { mutableStateOf<Pair<String, Long>?>(null) }
    var showMeteredDialog by remember(route) { mutableStateOf(false) }
    val isPortraitVideo = controller.videoSize.let { it != IntSize.Zero && it.height > it.width }

    val orientationListener = remember {
        mutableStateOf<(OrientationManager.ScreenOrientation) -> Unit>({})
    }
    val orientationManager = remember(activity) {
        OrientationManager(activity) { orientation -> orientationListener.value(orientation) }
    }
    val screenBridge = remember(activity, orientationManager) {
        AndroidPlayerScreenController(activity, orientationManager)
    }

    fun enterFullscreen() {
        if (isFullscreen) return
        isFullscreen = true
        screenBridge.onEnterFullscreen(portraitVideo = isPortraitVideo)
    }

    fun exitFullscreen() {
        if (!isFullscreen) return
        isFullscreen = false
        screenBridge.onExitFullscreen()
        // 全屏里调过的亮度还给系统（HJzvdStd :882-890 的快照还原语义）
        deviceControls.restoreSystemBrightness()
    }

    // 重力感应自动进出全屏（条件照旧宿主 :359-375；2 秒防抖只作用于进入）
    SideEffect {
        orientationListener.value = listener@{ orientation ->
            if (Preferences.tabletMode) return@listener
            val visual = playbackVisualOf(controller)
            val playingOrPaused =
                visual == PlaybackVisual.Playing || visual == PlaybackVisual.Paused
            if (!playingOrPaused || isPortraitVideo) return@listener
            if (orientation.isLandscape && !isFullscreen) {
                val now = System.currentTimeMillis()
                if (now - lastAutoFullscreenAt > 2000) {
                    lastAutoFullscreenAt = now
                    enterFullscreen()
                }
            } else if (orientation == OrientationManager.ScreenOrientation.PORTRAIT && isFullscreen) {
                exitFullscreen()
            }
        }
    }

    DisposableEffect(lifecycleOwner, orientationManager) {
        lifecycleOwner.lifecycle.addObserver(orientationManager)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(orientationManager)
            orientationManager.disable()
        }
    }

    // 全屏状态下直接退出页面：方向与系统栏必须解锁
    DisposableEffect(screenBridge) {
        onDispose {
            if (isFullscreen) {
                screenBridge.onExitFullscreen()
            }
        }
    }

    // 竖屏视频入场动画：SurfaceView 不吃 graphicsLayer，动容器高度 0.5→1（300ms 减速）
    LaunchedEffect(isFullscreen) {
        if (isFullscreen && isPortraitVideo) {
            fullscreenEnterFraction.snapTo(0.5f)
            fullscreenEnterFraction.animateTo(1f, tween(300, easing = DecelerateEasing))
        } else {
            fullscreenEnterFraction.snapTo(1f)
        }
    }

    BackHandler(enabled = isFullscreen) { exitFullscreen() }

    fun buildPipParams(sourceRect: android.graphics.Rect?): PictureInPictureParams {
        val isPlaying = controller.isPlaying
        val icon = Icon.createWithResource(
            activity,
            if (isPlaying) R.drawable.ic_pip_pause_24 else R.drawable.ic_pip_play_arrow_24,
        )
        val intent = PendingIntent.getBroadcast(
            activity,
            0,
            Intent(MainActivity.ACTION_TOGGLE_PLAY).setPackage(activity.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val action = RemoteAction(
            icon,
            activity.getString(R.string.play_pause),
            activity.getString(R.string.play_pause),
            intent,
        )
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(listOf(action))
            .apply { if (sourceRect != null) setSourceRectHint(sourceRect) }
            .build()
    }

    // PiP 中播放状态变化时刷新 RemoteAction 图标（旧宿主 updatePipAction）
    LaunchedEffect(controller.isPlaying, hostUiState.isInPipMode) {
        if (hostUiState.isInPipMode) {
            activity.setPictureInPictureParams(buildPipParams(null))
        }
    }

    // ---- 平板高度（滚动折叠已按用户决定移除：播放器位置固定，下方内容自行滚动。
    //      折叠时容器高度变化会经 SurfaceHolder.surfaceChanged 改写 mpv 的
    //      android-surface-size，在缩小状态切全屏曾出现画面残缺——固定高度从根上消除该链条）----
    val configuration = LocalConfiguration.current
    var isSideRelatedCollapsed by remember { mutableStateOf(false) }
    val isTabletLandscape = Preferences.tabletMode &&
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val basePlayerHeight = when {
        isTabletLandscape -> if (isSideRelatedCollapsed) 500.dp else 400.dp
        Preferences.tabletMode -> 350.dp
        else -> 250.dp
    }

    val stringLongPressShare = remember(activity) {
        activity.getString(R.string.long_press_share_to_copy)
    }

    LaunchedEffect(Preferences.baseUrl) {
        val genreFile = if (Preferences.baseUrl == HanimeConstants.HANIME_URL[3]) {
            "genre_av.json"
        } else {
            "genre.json"
        }
        genres = loadBundledJson<List<SearchOption>>("files/search_options/$genreFile").orEmpty()
    }

    LaunchedEffect(route) {
        checkBadGuy(activity, R.raw.akarin)
    }

    LaunchedEffect(videoState, controller) {
        when (val state = videoState) {
            is VideoLoadingState.Success -> {
                title = state.info.title
                coverUrl = state.info.coverUrl
                videoUrls = state.info.videoUrls
                // 先读进度再插历史：insert 若是 REPLACE 会把 progress 清零，
                // 读在前才能拿到真正的续播位置（旧宿主两者是并发的，这里定序）
                val resume = DatabaseRepo.WatchHistory.findBy(route.videoCode)?.progress ?: 0L
                savedProgressMs = resume
                if (!viewModel.fromDownload) {
                    viewModel.insertWatchHistoryWithCover(
                        WatchHistoryEntity(
                            state.info.coverUrl,
                            state.info.title,
                            state.info.uploadTimeMillis,
                            kotlin.time.Clock.System.now().toEpochMilliseconds(),
                            route.videoCode,
                        )
                    )
                }
                val picked = state.info.videoUrls.pickPreferredEntry()
                if (picked == null) {
                    hasPlayableSource = false
                    showShortToast(R.string.fail_to_get_video_link)
                } else {
                    hasPlayableSource = true
                    playingQuality = picked.first
                    val startPosition = if (Preferences.allowResumePlayback) resume else 0L
                    // 流量提醒只在首播检查一次（进程内确认过就不再弹）；
                    // 播放中切到流量不再监听——CONNECTIVITY_CHANGE 已废弃，断流靠出错重试兜底
                    val needMeteredWarning = !viewModel.fromDownload &&
                            !Preferences.disableMobileDataWarning &&
                            !MobileDataWarningSession.accepted &&
                            environment.isNetworkMetered()
                    if (needMeteredWarning) {
                        // 默认不自动播放，所以进页不弹提醒；用户点播放钮时再弹（见 onBlockedPlayClick）
                        pendingMeteredPlay = picked.second to startPosition
                    } else {
                        // 进入页面默认停在暂停态：只 load（prepare 出首帧），不 play
                        controller.load(picked.second, startPosition)
                    }
                }
            }

            is VideoLoadingState.Error -> {
                state.throwable.localizedTextOrNull()?.let { showShortToast(it) }
                if (state.throwable is ParseException) {
                    activity.browse(getHanimeVideoLink(route.videoCode))
                }
            }

            is VideoLoadingState.NoContent ->
                showShortToast(R.string.video_might_not_exist)

            else -> Unit
        }
    }

    // 进度写回 + 后台暂停（对应旧宿主的 lifecycleObserver；goOnPlayOnPause 换成 controller.pause）
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // position 必须在回调里同步读完（进了协程后 controller 可能已 release）
                    val progress = controller.positionMs
                    scope.launch {
                        DatabaseRepo.WatchHistory.updateProgress(route.videoCode, progress)
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    if (!activity.isInPictureInPictureMode) {
                        exitFullscreen()
                    }
                    controller.pause()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 播放中保持屏幕常亮（旧链路由 jzvd 的 JZUtils 做，这条链路要自己管）
    DisposableEffect(controller.isPlaying) {
        val window = activity.window
        if (controller.isPlaying) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.modifyHKeyframeFlow.collect { (_, message) ->
                showShortToast(getString(message.resource, *message.args.toTypedArray()))
            }
        }
    }

    LaunchedEffect(viewModel, route.videoCode) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.loadDownloadedFlow.collect { entity ->
                val newQuality = checkedQuality ?: return@collect
                pendingDownloadPrompt = DownloadPromptState(
                    newQuality = newQuality,
                    oldQuality = entity?.quality,
                )
            }
        }
    }

    val actions = remember(activity, scope, viewModel, genres) {
        VideoRouteActions(
            context = activity,
            scope = scope,
            viewModel = viewModel,
            genres = genres,
            requestStoragePermission = { onGranted, onDenied, onPermanentlyDenied ->
                (activity as PermissionRequester).requestStoragePermission(
                    onGranted = onGranted,
                    onDenied = onDenied,
                    onPermanentlyDenied = onPermanentlyDenied,
                )
            },
            onPendingDownloadPromptChange = { pendingDownloadPrompt = it },
            getCheckedQuality = { checkedQuality },
            setCheckedQuality = { checkedQuality = it },
            onStoragePermissionDenied = { activity.navBackStack.removeLastOrNull() },
            onDownloadPermissionDialogCancelled = { activity.navBackStack.removeLastOrNull() },
        )
    }

    /** PiP 那三个方法原来是用 Jzvd 术语写的，这里用 controller 重新表达。 */
    val pageHost = remember(controller) {
        object : VideoPageHost {
            override fun showCommentBadge(count: Int) = viewModel.setCommentBadgeCount(count)

            // 旧语义是 PLAYING || PAUSE：加载完（有时长）、没出错、没播完就允许进 PiP
            override fun shouldEnterPip(): Boolean =
                controller.isPlaying ||
                        (controller.durationMs > 0 && !controller.isEnded && controller.error == null)

            override fun enterPipMode() {
                val rect = playerBounds?.let {
                    android.graphics.Rect(
                        it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt(),
                    )
                }
                activity.enterPictureInPictureMode(buildPipParams(rect))
            }

            override fun onPipModeChanged(isInPip: Boolean) = viewModel.setPipMode(isInPip)

            override fun togglePlayPause() {
                if (controller.isPlaying) controller.pause() else controller.play()
                if (activity.isInPictureInPictureMode) {
                    activity.setPictureInPictureParams(buildPipParams(null))
                }
            }
        }
    }

    // 不注册的话 MainActivity 的 onUserLeaveHint / PiP 广播全拿到 null——
    // 按 Home 不进 PiP、PiP 里的播放暂停按钮失灵
    DisposableEffect(pageHost) {
        activity.registerCurrentVideoHost(pageHost)
        // 条件注销：叠页转场时旧页后销毁，不能顶掉新页刚注册的 host
        onDispose { activity.unregisterVideoHost(pageHost) }
    }

    val relatedItems =
        viewModel.hanimeVideoFlow.collectAsStateWithLifecycle().value?.relatedHanimes.orEmpty()
    val fillPlayer = isFullscreen || hostUiState.isInPipMode
    Media3VideoShellContent(
        isTabletMode = Preferences.tabletMode,
        forceMainOnly = fillPlayer,
        relatedItems = relatedItems,
        onHideRelatedInIntroChange = { viewModel.hideRelatedInIntro(it) },
        onSideRelatedCollapsedChange = { isSideRelatedCollapsed = it },
        onOpenVideo = { item -> activity.showVideoDetailFragment(item.videoCode) },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(Modifier.fillMaxSize()) {
        HanimeVideoPlayer(
            controller = controller,
            title = title,
            posterUrl = coverUrl,
            qualityKeys = videoUrls?.keys?.toList().orEmpty(),
            currentQuality = playingQuality,
            onSelectQuality = { key ->
                val link = videoUrls?.get(key)?.link?.takeIf { it.isNotBlank() }
                if (link != null) {
                    // changeUrl 语义：换清晰度保留进度与播放状态
                    val position = controller.positionMs
                    val wasPlaying = controller.isPlaying
                    playingQuality = key
                    controller.load(link, position)
                    if (wasPlaying) controller.play()
                }
            },
            hKeyframes = hKeyframes,
            savedProgressMs = savedProgressMs,
            isFullscreen = isFullscreen,
            onToggleFullscreen = {
                if (isFullscreen) exitFullscreen() else enterFullscreen()
            },
            onBack = { activity.onBackPressedDispatcher.onBackPressed() },
            onGoHome = {
                exitFullscreen()
                activity.navBackStack.popTo(HomeRoute)
            },
            isInPip = hostUiState.isInPipMode,
            // 流量提醒待确认时也走「点播放钮再弹」的路径（jzvd clickStart 语义）
            hasPlayableSource = hasPlayableSource && pendingMeteredPlay == null,
            onBlockedPlayClick = {
                if (pendingMeteredPlay != null) {
                    showMeteredDialog = true
                } else {
                    showShortToast(R.string.fail_to_get_video_link)
                    activity.browse(getHanimeVideoLink(route.videoCode))
                }
            },
            onRetry = { viewModel.getHanimeVideo() },
            onRequestAddKeyframe = { position ->
                showAddHKeyframeDialog = position to (title.ifBlank { "Untitled" })
            },
            environment = environment,
            showSuperResolution = controller is SuperResolutionController,
            superResolutionIndex = superResolutionIndex,
            onSelectSuperResolution = { index ->
                superResolutionIndex = index
                (controller as? SuperResolutionController)?.setSuperResolution(index)
            },
            modifier = (if (fillPlayer) {
                // 全屏/PiP 铺满；竖屏视频入场时高度 0.5→1（顶部对齐即 pivotY=0 语义）
                Modifier.fillMaxWidth().fillMaxHeight(fullscreenEnterFraction.value)
            } else {
                // 非全屏时黑底垫过状态栏，控件从状态栏之下开始，不与系统栏文字重叠
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .statusBarsPadding()
                    .height(basePlayerHeight)
            }).onGloballyPositioned { playerBounds = it.boundsInWindow() },
        )
        if (!fillPlayer) VideoRouteContent(
            videoCode = route.videoCode,
            videoState = videoState,
            videoViewModel = viewModel,
            commentViewModel = commentViewModel,
            fromDownload = viewModel.fromDownload,
            pendingDownloadPrompt = pendingDownloadPrompt,
            onPendingDownloadPromptChange = { pendingDownloadPrompt = it },
            onRetry = { viewModel.getHanimeVideo() },
            onOpenVideo = { item -> activity.showVideoDetailFragment(item.videoCode) },
            onOpenArtist = actions::openArtistSearch,
            onNavigateToSearch = actions::openTagSearch,
            onToggleSubscribe = actions::toggleArtistSubscription,
            onToggleFavorite = actions::toggleFavorite,
            onRateVideo = actions::rateVideo,
            onManageMyList = actions::updateMyListSelection,
            onQuickCheckIn = { record ->
                // \u001E 是打卡记录里「标题 / videoCode」的分隔符
                val sep = "\u001E"
                val normalizedRecord = if (record.sideDishes.contains(sep)) {
                    record
                } else {
                    record.copy(sideDishes = "${record.sideDishes}$sep${route.videoCode}")
                }
                scope.launch(Dispatchers.IO) {
                    CheckInRecordDatabase.instance.checkInDao().insert(normalizedRecord)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, R.string.checkin, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onPrepareDownload = { quality, video ->
                checkedQuality = quality
                video?.let(actions::startDownloadFlow)
            },
            onConfirmDownloadPrompt = { video ->
                video?.let { actions.confirmPendingDownload(it, pendingDownloadPrompt) }
            },
            onRequestOpenOfficialDownloadPage = actions::openOfficialDownloadPage,
            onRequestOpenDownloadPermissionSettings = actions::openDownloadPermissionSettings,
            onOpenWebPage = actions::openVideoWebPage,
            onOpenOriginalComic = actions::openOriginalComic,
            onOpenShare = { content, shareTitle -> shareText(content, shareTitle) },
            onCopyText = {
                it.copyToClipboard()
                showShortToast(R.string.copy_to_clipboard)
            },
            onIntroductionLinkClick = actions::openIntroductionLink,
            stringLongPressShare = stringLongPressShare,
            pageHost = pageHost,
        )
        }
    }

    if (showMeteredDialog) {
        ConfirmDialog(
            visible = true,
            title = stringResource(Res.string.warning),
            message = stringResource(Res.string.player_tips_not_wifi),
            confirmText = stringResource(Res.string.player_tips_not_wifi_confirm),
            dismissText = stringResource(Res.string.player_tips_not_wifi_cancel),
            onConfirm = {
                MobileDataWarningSession.accepted = true
                pendingMeteredPlay?.let { (url, startPosition) ->
                    controller.load(url, startPosition)
                    controller.play()
                }
                pendingMeteredPlay = null
                showMeteredDialog = false
            },
            onDismiss = { showMeteredDialog = false },
        )
    }

    showAddHKeyframeDialog?.let { (currentPosition, keyframeTitle) ->
        ConfirmDialog(
            visible = true,
            title = activity.getString(R.string.add_to_h_keyframe),
            message = buildString {
                appendLine(activity.getString(R.string.sure_to_add_to_h_keyframe))
                append(activity.getString(R.string.current_position_d_ms, currentPosition))
            },
            confirmText = activity.getString(R.string.confirm),
            dismissText = activity.getString(R.string.cancel),
            onConfirm = {
                viewModel.appendHKeyframe(
                    route.videoCode,
                    keyframeTitle,
                    HKeyframeEntity.Keyframe(position = currentPosition, prompt = null),
                )
                Firebase.analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT) {
                    param(FirebaseAnalytics.Param.ITEM_ID, FirebaseConstants.H_KEYFRAMES)
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, FirebaseConstants.H_KEYFRAMES)
                }
                showAddHKeyframeDialog = null
            },
            onDismiss = { showAddHKeyframeDialog = null },
        )
    }
}

/**
 * 按 `Preferences.videoQuality` 选清晰度，找不到就用第一条。返回 (清晰度 key, 链接)。
 *
 * 旧实现（`HJzvdStd:456-471`）绕了一大圈：`urlsMap.keys.indexOf(quality)` 拿到的是
 * `Int?`，再和 `-1` 比——null 的时候 `null != -1` 为真，会走进「找到了」的分支再靠
 * 内层 null 检查兜住。这里直接按 key 取，找不到显式回落。
 */
/** `DecelerateInterpolator(1f)` 的等价 easing（HJzvdStd :975-986 的入场动画曲线）。 */
private val DecelerateEasing = Easing { fraction -> 1f - (1f - fraction) * (1f - fraction) }

private fun ResolutionLinkMap.pickPreferredEntry(): Pair<String, String>? {
    if (isEmpty()) return null
    val preferred = Preferences.videoQuality
    val key = if (containsKey(preferred)) preferred else keys.first()
    val link = this[key]?.link?.takeIf { it.isNotBlank() } ?: return null
    return key to link
}
