package io.github.darriousliu.han1meviewer.ui.screen.video

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import io.github.darriousliu.han1meviewer.ui.bridge.VideoPageHost
import io.github.darriousliu.han1meviewer.core.navigation.VideoRoute
import io.github.darriousliu.han1meviewer.ui.screen.video.player.HanimeVideoPlayer
import io.github.darriousliu.han1meviewer.ui.screen.video.player.rememberVideoPlayerController
import io.github.darriousliu.han1meviewer.feature.comment.CommentViewModel
import io.github.darriousliu.han1meviewer.ui.viewmodel.VideoViewModel
import io.github.darriousliu.han1meviewer.core.common.util.copyToClipboard
import io.github.darriousliu.han1meviewer.core.common.util.loadBundledJson
import io.github.darriousliu.han1meviewer.core.common.util.localizedTextOrNull
import io.github.darriousliu.han1meviewer.util.shareText
import io.github.darriousliu.han1meviewer.util.showShortToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
@Composable
fun Media3VideoRouteHostScreen(
    activity: MainActivity,
    route: VideoRoute,
) {
    val scope = rememberCoroutineScope()
    val viewModel: VideoViewModel = viewModel(viewModelStoreOwner = activity)
    // 评论 VM 跟着 NavEntry 走（和旧宿主 VideoRouteHostScreen:100 一致），
    // 一个视频一个实例，code 构造时定死——不能挂 Activity scope：
    // 那样第二个视频会拿到第一个视频的实例，factory 被静默忽略，评论就串台了。
    val commentViewModel: CommentViewModel = viewModel(
        factory = CommentViewModel.factory(route.videoCode),
    )
    val controller = rememberVideoPlayerController(route.videoCode to route.localUri)
    val videoState by viewModel.hanimeVideoStateFlow.collectAsStateWithLifecycle()

    var title by remember(route) { mutableStateOf("") }
    var checkedQuality by remember(route) { mutableStateOf<String?>(null) }
    var pendingDownloadPrompt by remember(route) { mutableStateOf<DownloadPromptState?>(null) }
    var genres by remember(Preferences.baseUrl) { mutableStateOf(emptyList<SearchOption>()) }

    val stringLongPressShare = remember(activity) {
        activity.getString(R.string.long_press_share_to_copy)
    }

    // 组合期同步赋值：下面 LaunchedEffect 里的 getHanimeVideo 依赖它选本地缓存还是网络，
    // tab 列表也要靠它决定挂不挂评论页。旧宿主 VideoRouteHostScreen:126 同样写在组合体里。
    viewModel.fromDownload = route.videoCode == "-1" || route.localUri != null

    LaunchedEffect(Preferences.baseUrl) {
        val genreFile = if (Preferences.baseUrl == HanimeConstants.HANIME_URL[3]) {
            "genre_av.json"
        } else {
            "genre.json"
        }
        genres = loadBundledJson<List<SearchOption>>("files/search_options/$genreFile").orEmpty()
    }

    LaunchedEffect(route) {
        viewModel.getHanimeVideo(route.videoCode, route.localUri)
    }

    LaunchedEffect(videoState, controller) {
        when (val state = videoState) {
            is VideoLoadingState.Success -> {
                title = state.info.title
                val url = state.info.videoUrls.pickPreferredUrl()
                if (url == null) {
                    showShortToast(R.string.fail_to_get_video_link)
                } else {
                    val resume = DatabaseRepo.WatchHistory.findBy(route.videoCode)?.progress ?: 0L
                    controller.load(url, if (Preferences.allowResumePlayback) resume else 0L)
                    controller.play()
                }
            }

            is VideoLoadingState.Error ->
                state.throwable.localizedTextOrNull()?.let { showShortToast(it) }

            else -> Unit
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
            override fun shouldEnterPip(): Boolean = controller.isPlaying
            override fun enterPipMode() = Unit          // 25-5 补
            override fun onPipModeChanged(isInPip: Boolean) = viewModel.setPipMode(isInPip)
            override fun togglePlayPause() {
                if (controller.isPlaying) controller.pause() else controller.play()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        HanimeVideoPlayer(
            controller = controller,
            title = title,
            onBack = { activity.onBackPressedDispatcher.onBackPressed() },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
        VideoRouteContent(
            videoCode = route.videoCode,
            videoState = videoState,
            videoViewModel = viewModel,
            commentViewModel = commentViewModel,
            fromDownload = viewModel.fromDownload,
            pendingDownloadPrompt = pendingDownloadPrompt,
            onPendingDownloadPromptChange = { pendingDownloadPrompt = it },
            onRetry = { viewModel.getHanimeVideo(route.videoCode, route.localUri) },
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

/**
 * 按 `Preferences.videoQuality` 选清晰度，找不到就用第一条。
 *
 * 旧实现（`HJzvdStd:456-471`）绕了一大圈：`urlsMap.keys.indexOf(quality)` 拿到的是
 * `Int?`，再和 `-1` 比——null 的时候 `null != -1` 为真，会走进「找到了」的分支再靠
 * 内层 null 检查兜住。这里直接按 key 取，找不到显式回落。
 */
private fun ResolutionLinkMap.pickPreferredUrl(): String? {
    if (isEmpty()) return null
    val preferred = Preferences.videoQuality
    return (this[preferred] ?: values.first()).link.takeIf { it.isNotBlank() }
}
