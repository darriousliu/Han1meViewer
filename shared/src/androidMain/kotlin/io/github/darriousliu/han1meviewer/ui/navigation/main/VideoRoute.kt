package io.github.darriousliu.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.darriousliu.han1meviewer.PlayerDefaults
import io.github.darriousliu.han1meviewer.Preferences
import io.github.darriousliu.han1meviewer.ui.activity.MainActivity
import io.github.darriousliu.han1meviewer.ui.navigation.VideoRoute
import io.github.darriousliu.han1meviewer.ui.screen.video.Media3VideoRouteHostScreen
import io.github.darriousliu.han1meviewer.ui.screen.video.VideoRouteHostScreen

/**
 * 播放器内核的分叉点。
 *
 * 在这里（14 行的转发器）分，而不是把 719 行的 [VideoRouteHostScreen] 抽成共用接口——
 * 那样会改到正在正常工作的路径，而这个环境验证不了播放。旧路径字节级不变。
 *
 * ⚠️ `remember` 无 key：切完内核要**退出视频页再进**才生效。这是既有行为
 * （旧宿主读 `Preferences.switchPlayerKernel` 也是 `remember {}`），本轮不改。
 */
@Composable
fun VideoRouteScreen(
    activity: MainActivity,
    route: VideoRoute,
) {
    val useCompose = remember {
        Preferences.switchPlayerKernel == PlayerDefaults.KERNEL_EXO_COMPOSE
    }
    if (useCompose) {
        Media3VideoRouteHostScreen(activity = activity, route = route)
    } else {
        VideoRouteHostScreen(activity = activity, route = route)
    }
}
