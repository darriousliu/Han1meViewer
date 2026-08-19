package io.github.darriousliu.han1meviewer.feature.video.player

/**
 * 播放器对「屏幕/窗口」的诉求，由宿主实现（Android 是 Activity 层的
 * 系统栏隐藏、方向锁定、PiP）。方法全带默认空实现，无该能力的平台零代码。
 *
 * 作为参数传入 [HanimeVideoPlayer]，不做 expect/actual——它天然是宿主的事。
 */
interface PlayerScreenController {

    /** 进全屏。[portraitVideo] 为 true 时锁竖屏，否则锁横屏。 */
    fun onEnterFullscreen(portraitVideo: Boolean) {}

    fun onExitFullscreen() {}

    /** 请求进入画中画。 */
    fun requestPip() {}
}

object NoopPlayerScreenController : PlayerScreenController
