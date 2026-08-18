package com.yenaly.han1meviewer.ui.screen.video.player

import androidx.compose.runtime.Composable

/**
 * 播放器手势要用的两个**设备**能力：屏幕亮度和媒体音量。
 *
 * 它们和播放器本身无关（调的是系统），所以和 [VideoPlayerController] 分开——
 * 将来桌面端可能有音量没亮度，iOS 亮度要 `UIScreen.brightness`。
 *
 * 同样全带默认实现，未落地的平台用 [NoopDeviceMediaControls]。
 */
interface DeviceMediaControls {

    /**
     * 当前窗口亮度，`0f..1f`。
     *
     * ⚠️ 语义照抄 `HJzvdStd`：**下限是 0.01f 不是 0f**（0 在某些机型上是「跟随系统」
     * 而不是「最暗」），所以调到底也留一点。
     */
    var brightness: Float
        get() = DEFAULT_BRIGHTNESS
        set(_) {}

    /** 媒体音量占比，`0f..1f`。 */
    var volumePercent: Float
        get() = 0f
        set(_) {}

    /** 离开播放页时把亮度还给系统。 */
    fun restoreSystemBrightness() {}

    companion object {
        const val DEFAULT_BRIGHTNESS = 0.5f

        /** `HJzvdStd` 里那个「必须自己过滤负值」的下限。 */
        const val MIN_BRIGHTNESS = 0.01f
    }
}

object NoopDeviceMediaControls : DeviceMediaControls

@Composable
expect fun rememberDeviceMediaControls(): DeviceMediaControls
