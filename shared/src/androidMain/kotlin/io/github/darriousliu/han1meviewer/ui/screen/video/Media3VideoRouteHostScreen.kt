package io.github.darriousliu.han1meviewer.ui.screen.video

import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.darriousliu.han1meviewer.core.common.HanimeConstants
import io.github.darriousliu.han1meviewer.PermissionRequester
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.R
import io.github.darriousliu.han1meviewer.core.common.ResolutionLinkMap
import io.github.darriousliu.han1meviewer.core.repository.DatabaseRepo
import io.github.darriousliu.han1meviewer.core.storage.dao.CheckInRecordDatabase
import io.github.darriousliu.han1meviewer.core.model.SearchOption
import io.github.darriousliu.han1meviewer.core.common.state.VideoLoadingState
import io.github.darriousliu.han1meviewer.ui.activity.MainActivity
import io.github.darriousliu.han1meviewer.feature.video.VideoPageHost
import io.github.darriousliu.han1meviewer.core.navigation.VideoRoute
import io.github.darriousliu.han1meviewer.feature.video.player.HanimeVideoPlayer
import io.github.darriousliu.han1meviewer.feature.video.player.rememberVideoPlayerController
import io.github.darriousliu.han1meviewer.feature.comment.CommentViewModel
import io.github.darriousliu.han1meviewer.feature.video.VideoViewModel
import io.github.darriousliu.han1meviewer.core.common.exception.ParseException
import io.github.darriousliu.han1meviewer.core.firebase.FirebaseConstants
import io.github.darriousliu.han1meviewer.core.navigation.HomeRoute
import io.github.darriousliu.han1meviewer.core.navigation.popTo
import io.github.darriousliu.han1meviewer.core.storage.entity.HKeyframeEntity
import io.github.darriousliu.han1meviewer.core.ui.component.ConfirmDialog
import io.github.darriousliu.han1meviewer.core.common.util.copyToClipboard
import io.github.darriousliu.han1meviewer.core.common.util.loadBundledJson
import io.github.darriousliu.han1meviewer.core.common.util.localizedTextOrNull
import io.github.darriousliu.han1meviewer.core.storage.entity.WatchHistoryEntity
import io.github.darriousliu.han1meviewer.core.storage.getHanimeVideoLink
import io.github.darriousliu.han1meviewer.util.browse
import io.github.darriousliu.han1meviewer.util.checkBadGuy
import io.github.darriousliu.han1meviewer.util.shareText
import io.github.darriousliu.han1meviewer.util.showShortToast
import org.jetbrains.compose.resources.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.darriousliu.han1meviewer.feature.video.DownloadPromptState
import io.github.darriousliu.han1meviewer.feature.video.VideoRouteContent

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
 * 和旧宿主的一处**结构差异**：这里是纯 Compose 的 `Column`（播放器在上、tab 在下），
 * 没有 `CoordinatorLayout`/`AppBarLayout`。所以**滚动列表不会折叠播放器区域**——
 * 那正是旧路径要靠 View 互操作（`rememberHostNestedScrollConnection`）才能做到的事。
 * 这是本轮已知的行为差异，后续再补。
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
    val hKeyframes by viewModel.hKeyframes.collectAsStateWithLifecycle()
    val hostUiState by viewModel.videoHostUiStateFlow.collectAsStateWithLifecycle()

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
                    controller.load(picked.second, if (Preferences.allowResumePlayback) resume else 0L)
                    controller.play()
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

                Lifecycle.Event.ON_STOP -> controller.pause()

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

            override fun enterPipMode() = Unit          // 25-5 补
            override fun onPipModeChanged(isInPip: Boolean) = viewModel.setPipMode(isInPip)
            override fun togglePlayPause() {
                if (controller.isPlaying) controller.pause() else controller.play()
            }
        }
    }

    // 不注册的话 MainActivity 的 onUserLeaveHint / PiP 广播全拿到 null——
    // 按 Home 不进 PiP、PiP 里的播放暂停按钮失灵
    DisposableEffect(pageHost) {
        activity.registerCurrentVideoHost(pageHost)
        onDispose { activity.registerCurrentVideoHost(null) }
    }

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
                    // changeUrl 语义：换清晰度保留进度
                    val position = controller.positionMs
                    playingQuality = key
                    controller.load(link, position)
                    controller.play()
                }
            },
            hKeyframes = hKeyframes,
            savedProgressMs = savedProgressMs,
            isFullscreen = false,
            onToggleFullscreen = { /* P3 全屏批接线 */ },
            onBack = { activity.onBackPressedDispatcher.onBackPressed() },
            onGoHome = { activity.navBackStack.popTo(HomeRoute) },
            isInPip = hostUiState.isInPipMode,
            hasPlayableSource = hasPlayableSource,
            onBlockedPlayClick = {
                showShortToast(R.string.fail_to_get_video_link)
                activity.browse(getHanimeVideoLink(route.videoCode))
            },
            onRetry = { viewModel.getHanimeVideo() },
            onRequestAddKeyframe = { position ->
                showAddHKeyframeDialog = position to (title.ifBlank { "Untitled" })
            },
            modifier = Modifier.fillMaxWidth().height(250.dp),
        )
        VideoRouteContent(
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
private fun ResolutionLinkMap.pickPreferredEntry(): Pair<String, String>? {
    if (isEmpty()) return null
    val preferred = Preferences.videoQuality
    val key = if (containsKey(preferred)) preferred else keys.first()
    val link = this[key]?.link?.takeIf { it.isNotBlank() } ?: return null
    return key to link
}
