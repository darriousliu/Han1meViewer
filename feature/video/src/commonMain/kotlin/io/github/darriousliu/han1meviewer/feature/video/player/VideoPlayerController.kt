package io.github.darriousliu.han1meviewer.feature.video.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize

/**
 * 一次播放会话的能力面。
 *
 * Step 25 建立，走的是 Step 22 那套「能力接口在 commonMain + 各平台 actual」的结构。
 * 实现方**拥有**底层播放器，也负责它的生命周期（[release]）。
 *
 * 所有方法都带默认实现，所以还没落地的平台一行就能给出 [NoopVideoPlayerController]；
 * 每落地一个平台就加对应的 override。**本轮只实现 Android（Media3 ExoPlayer）。**
 */
interface VideoPlayerController {

    val isPlaying: Boolean get() = false
    val isBuffering: Boolean get() = false

    val positionMs: Long get() = 0L

    /**
     * 时长。**未就绪时必须给 0**，别把 ExoPlayer 的 `C.TIME_UNSET`
     * （`Long.MIN_VALUE + 1`）漏给 UI——进度条会拿到 NaN。
     */
    val durationMs: Long get() = 0L

    val bufferedPositionMs: Long get() = 0L

    /** 视频实际尺寸，用来判竖屏视频（全屏时要走另一套布局）。未知时 [IntSize.Zero]。 */
    val videoSize: IntSize get() = IntSize.Zero

    val error: Throwable? get() = null

    /** 完播态（播到末尾）。[load] 后清零；seek 离开末尾也会变回 false。 */
    val isEnded: Boolean get() = false

    /** 首帧已渲染；封面在此之后隐藏。[load] 后清零。 */
    val firstFrameRendered: Boolean get() = false

    /** @param startPositionMs 续播位置；0 表示从头 */
    fun load(url: String, startPositionMs: Long = 0L) {}

    fun play() {}
    fun pause() {}
    fun seekTo(positionMs: Long) {}

    fun setSpeed(speed: Float) {}

    /**
     * 长按临时加速；抬手调 [restoreSpeed] 还原。
     * media3 自带 `temporarilyOverrideSpeedWith`/`restoreOverriddenSpeed`，
     * 不用像 `HJzvdStd` 那样自己记录原速度。
     */
    fun boostSpeed(multiplier: Float) {}
    fun restoreSpeed() {}

    fun release() {}
}

/** 还没落地播放能力的平台用这个。 */
object NoopVideoPlayerController : VideoPlayerController

/**
 * 建一个播放会话。[key] 变化时重建（通常是 videoCode + localUri）。
 * 实现方负责在离开组合时 [VideoPlayerController.release]。
 */
@Composable
expect fun rememberVideoPlayerController(key: Any?): VideoPlayerController

/**
 * 渲染表面。
 *
 * Android 上是 media3 的 `PlayerSurface`（`SurfaceView`）。
 * ⚠️ `SurfaceView` **不吃 `graphicsLayer` 变换**——想做缩放动画得动容器尺寸，
 * 不能对它加 `scaleY`。
 *
 * 其余平台先画一块黑底占位。
 */
@Composable
expect fun VideoSurface(controller: VideoPlayerController, modifier: Modifier)
