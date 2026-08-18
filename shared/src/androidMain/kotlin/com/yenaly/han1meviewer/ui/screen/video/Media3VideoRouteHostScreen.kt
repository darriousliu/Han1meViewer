package com.yenaly.han1meviewer.ui.screen.video

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.ResolutionLinkMap
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.navigation.VideoRoute
import com.yenaly.han1meviewer.ui.screen.video.player.HanimeVideoPlayer
import com.yenaly.han1meviewer.ui.screen.video.player.rememberVideoPlayerController
import com.yenaly.han1meviewer.ui.viewmodel.VideoViewModel
import com.yenaly.han1meviewer.util.localizedTextOrNull
import com.yenaly.han1meviewer.util.showShortToast

/**
 * Media3 + Compose 的视频页宿主。
 *
 * ⚠️ **Step 25-2 的最小可播版本**：只有播放器本身，**还没有简介/评论那两个 tab**。
 * 它存在的目的是先把「只有真机能回答的未知」一次试掉：surface 挂不挂得上、
 * HLS 能不能放、代理通不通、position 报得准不准。tab 和完整控件在后续几批补。
 *
 * 和旧的 [VideoRouteHostScreen] **完全隔离**——旧路径一个字没动，
 * 在设置页把内核切回去就能回到它。所以这里刻意重复了一小段 ViewModel/加载接线，
 * 而不是把那 719 行抽成共用接口：改坏了只能整批回退的前提下，隔离比 DRY 值钱。
 *
 * ⚠️ 本文件**绝不能**调 `Jzvd.releaseAllVideos()` / `goOnPlayOnPause()` / `backPress()`——
 * 那三个是全局静态，两套播放器共用会互相踩。
 */
@Composable
fun Media3VideoRouteHostScreen(
    activity: MainActivity,
    route: VideoRoute,
) {
    val viewModel: VideoViewModel = viewModel(viewModelStoreOwner = activity)
    val controller = rememberVideoPlayerController(route.videoCode to route.localUri)
    val videoState by viewModel.hanimeVideoStateFlow.collectAsStateWithLifecycle()

    var title by remember(route) { mutableStateOf("") }

    LaunchedEffect(route) {
        viewModel.getHanimeVideo(route.videoCode, route.localUri)
    }

    LaunchedEffect(videoState, controller) {
        when (val state = videoState) {
            is VideoLoadingState.Success -> {
                title = state.info.title
                val url = state.info.videoUrls.pickPreferredUrl()
                if (url == null) {
                    showShortToast(com.yenaly.han1meviewer.R.string.fail_to_get_video_link)
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

    HanimeVideoPlayer(
        controller = controller,
        title = title,
        onBack = { activity.onBackPressedDispatcher.onBackPressed() },
        modifier = Modifier.fillMaxSize(),
    )
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
