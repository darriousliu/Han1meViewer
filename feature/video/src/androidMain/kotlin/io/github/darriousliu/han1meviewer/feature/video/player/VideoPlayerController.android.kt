package io.github.darriousliu.han1meviewer.feature.video.player

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW

/**
 * Media3 ExoPlayer 实现。
 *
 * 从 `ui/view/video/HMediaKernel.kt` 的 `ExoMediaKernel` 搬来两处**必须保留**的：
 *
 * 1. `DefaultDataSource.Factory` 包 `DefaultHttpDataSource.Factory()`——后者走
 *    `HttpURLConnection`，因此吃 `HanimeApplication` 里的进程级 `ProxySelector`
 *    （Step 23 特意没把 Android 的代理改成 per-client，就是为了它和 WebView）。
 *    `DefaultDataSource` 同时负责 `content://`/`file://`，下载的视频要靠它。
 * 2. `.m3u8` 子串判断 → `HlsMediaSource`，否则 `ProgressiveMediaSource`。
 *    **逐字照搬**，不要换成 `DefaultMediaSourceFactory` 的自动推断——
 *    带 query string 的 URL 推断结果不一样。
 *
 * 明确**丢掉**的：`HandlerThread("JZVD")` 和 `runOnPlayerThread`。那是迁就 jzvd 的
 * 线程模型，而且它在**主线程**上 `CountDownLatch.await(300ms)`，是个 ANR 源；
 * 顺带那几个全默认参数构造的 TrackSelector/LoadControl/BandwidthMeter/RenderersFactory
 * 也去掉了——和 `ExoPlayer.Builder(context)` 内部建的完全一样。
 */
@OptIn(UnstableApi::class)
private class Media3VideoPlayerController(
    /** `PlayerSurface` 要的是 media3 的 `Player`，所以这里不设 private。 */
    val exoPlayer: ExoPlayer,
    private val dataSourceFactory: DefaultDataSource.Factory,
) : VideoPlayerController, Player.Listener {

    private var playingState by mutableStateOf(false)
    private var bufferingState by mutableStateOf(false)
    private var sizeState by mutableStateOf(IntSize.Zero)
    private var errorState by mutableStateOf<Throwable?>(null)

    override val isPlaying: Boolean get() = playingState
    override val isBuffering: Boolean get() = bufferingState
    override val videoSize: IntSize get() = sizeState
    override val error: Throwable? get() = errorState

    override val positionMs: Long get() = exoPlayer.currentPosition.coerceAtLeast(0L)
    override val bufferedPositionMs: Long get() = exoPlayer.bufferedPosition.coerceAtLeast(0L)

    /** `C.TIME_UNSET` 是 `Long.MIN_VALUE + 1`，绝不能漏给 UI。 */
    override val durationMs: Long
        get() = exoPlayer.duration.let { if (it == C.TIME_UNSET) 0L else it }

    init {
        exoPlayer.addListener(this)
    }

    override fun load(url: String, startPositionMs: Long) {
        errorState = null
        val item = MediaItem.fromUri(url)
        val source = if (url.contains(".m3u8")) {
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(item)
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(item)
        }
        exoPlayer.setMediaSource(source)
        if (startPositionMs > 0L) exoPlayer.seekTo(startPositionMs)
        exoPlayer.prepare()
    }

    override fun play() { exoPlayer.play() }
    override fun pause() { exoPlayer.pause() }
    override fun seekTo(positionMs: Long) { exoPlayer.seekTo(positionMs.coerceAtLeast(0L)) }

    override fun setSpeed(speed: Float) { exoPlayer.setPlaybackSpeed(speed) }

    private var speedBeforeBoost: Float? = null

    override fun boostSpeed(multiplier: Float) {
        if (speedBeforeBoost != null) return
        speedBeforeBoost = exoPlayer.playbackParameters.speed
        exoPlayer.setPlaybackSpeed(exoPlayer.playbackParameters.speed * multiplier)
    }

    override fun restoreSpeed() {
        speedBeforeBoost?.let { exoPlayer.setPlaybackSpeed(it) }
        speedBeforeBoost = null
    }

    override fun release() {
        exoPlayer.removeListener(this)
        exoPlayer.release()
    }

    // ---- Player.Listener ----

    override fun onIsPlayingChanged(isPlaying: Boolean) { playingState = isPlaying }

    override fun onPlaybackStateChanged(playbackState: Int) {
        bufferingState = playbackState == Player.STATE_BUFFERING
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        // 照搬 ExoMediaKernel：宽要乘 pixelWidthHeightRatio 才是显示宽
        sizeState = IntSize(
            (videoSize.width * videoSize.pixelWidthHeightRatio).toInt(),
            videoSize.height,
        )
    }

    override fun onPlayerError(error: PlaybackException) { errorState = error }
}

@OptIn(UnstableApi::class)
@Composable
actual fun rememberVideoPlayerController(key: Any?): VideoPlayerController {
    val context = LocalContext.current
    val controller = remember(key) {
        val dataSourceFactory = DefaultDataSource.Factory(
            context,
            DefaultHttpDataSource.Factory(),
        )
        val player = ExoPlayer.Builder(context)
            // 原来是手写 AUDIOFOCUS_GAIN_TRANSIENT 且**不处理回调**（来电不会自动暂停）。
            // 交给 media3：它请求 GAIN 并会在失焦时自动暂停/闪避——更正确，但是行为变化。
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .build()
            .apply { repeatMode = Player.REPEAT_MODE_OFF }
        Media3VideoPlayerController(player, dataSourceFactory)
    }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    return controller
}

@OptIn(UnstableApi::class)
@Composable
actual fun VideoSurface(controller: VideoPlayerController, modifier: Modifier) {
    val exo = (controller as? Media3VideoPlayerController)?.exoPlayer ?: return
    PlayerSurface(exo, modifier, SURFACE_TYPE_SURFACE_VIEW)
}
