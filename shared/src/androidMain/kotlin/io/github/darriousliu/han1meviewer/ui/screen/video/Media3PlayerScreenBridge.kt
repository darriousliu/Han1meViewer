package io.github.darriousliu.han1meviewer.ui.screen.video

import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.darriousliu.han1meviewer.feature.video.player.PlayerScreenController
import io.github.darriousliu.han1meviewer.ui.activity.MainActivity
import io.github.darriousliu.han1meviewer.util.OrientationManager

/**
 * [PlayerScreenController] 的 Android 实现：全屏 = 隐藏系统栏 + 锁方向。
 * 竖屏视频锁竖屏、横屏视频锁横屏（HJzvdStd :989-998 语义）；
 * 退出时恢复系统栏并解锁方向。亮度快照的还原由 DeviceMediaControls 负责。
 */
class AndroidPlayerScreenController(
    private val activity: MainActivity,
    private val orientationManager: OrientationManager,
) : PlayerScreenController {

    private val insetsController: WindowInsetsControllerCompat
        get() = WindowCompat.getInsetsController(activity.window, activity.window.decorView)

    override fun onEnterFullscreen(portraitVideo: Boolean) {
        insetsController.run {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        orientationManager.lockOrientation(
            activity,
            if (portraitVideo) {
                OrientationManager.ScreenOrientation.PORTRAIT
            } else {
                OrientationManager.ScreenOrientation.LANDSCAPE
            },
        )
    }

    override fun onExitFullscreen() {
        insetsController.show(WindowInsetsCompat.Type.systemBars())
        orientationManager.unlockOrientation(activity)
    }
}
