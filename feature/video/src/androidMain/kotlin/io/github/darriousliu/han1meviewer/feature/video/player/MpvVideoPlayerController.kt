package io.github.darriousliu.han1meviewer.feature.video.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.Surface
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toUri
import io.github.darriousliu.han1meviewer.core.common.BuildConfig
import io.github.darriousliu.han1meviewer.core.common.USER_AGENT
import io.github.darriousliu.han1meviewer.core.network.HProxySelector
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import `is`.xyz.mpv.MPVLib

private const val TAG = "MpvVideoPlayerController"

/**
 * mpv 的 [VideoPlayerController] 实现，对照 jzvd 时代的 `MpvMediaKernel` 移植：
 * 选项表、fd:// 打开 content/file、事件观察者、超分（Anime4K glsl-shaders）全部照搬；
 * 去掉的是 jzvd 状态机回调——播放事实映射成 Compose state。
 *
 * ⚠️ **MPVLib 是进程级单例**（`create/init` 在 HanimeApplication），播放页叠播放页时
 * nav3 转场会让两个控制器短暂共存：后创建的实例经 [owner] 令牌**接管**会话，被接管的
 * 旧实例立即失效（只摘掉自己的观察者，不碰全局的 vo/surface/文件）；`release` 里的
 * 全局拆除（loadfile ""、vo=null、detachSurface）只有**仍是所有者**时才执行——
 * 否则旧页销毁会把新页刚加载的会话整个拆掉，两页互杀。
 *
 * 另：和 media3 一样不走 OkHttp——mpv 不吃进程级 `ProxySelector`，
 * HTTP 代理要单独喂 `http-proxy` 选项。
 */
@Stable
class MpvVideoPlayerController(
    private val context: Context,
) : VideoPlayerController, SuperResolutionController {

    companion object {
        /** 当前持有 MPVLib 会话的实例；创建新实例即接管。 */
        @Volatile
        private var owner: MpvVideoPlayerController? = null
    }

    private val handler = Handler(Looper.getMainLooper())

    private var playingState by mutableStateOf(false)
    private var bufferingState by mutableStateOf(false)
    private var endedState by mutableStateOf(false)
    private var firstFrameState by mutableStateOf(false)
    private var sizeState by mutableStateOf(IntSize.Zero)
    private var positionMsState by mutableLongStateOf(0L)
    private var durationMsState by mutableLongStateOf(0L)
    private var cacheDurationMsState by mutableLongStateOf(0L)

    override val isPlaying: Boolean get() = playingState
    override val isBuffering: Boolean get() = bufferingState
    override val isEnded: Boolean get() = endedState
    override val firstFrameRendered: Boolean get() = firstFrameState
    override val videoSize: IntSize get() = sizeState
    override val positionMs: Long get() = positionMsState
    override val durationMs: Long get() = durationMsState
    override val bufferedPositionMs: Long
        get() = (positionMsState + cacheDurationMsState).coerceAtMost(durationMsState)

    private var currentSpeed: Float = Preferences.playerSpeed
    private var speedBeforeBoost: Float? = null
    private var pendingSeekMs = 0L

    /** 已释放或已被新实例接管；此后不得再碰 MPVLib 的任何全局状态。 */
    @Volatile
    private var released = false

    private val isOwner: Boolean get() = owner === this

    // fd:// 打开 content/file 的句柄管理（照 MpvMediaKernel）
    private var currentPfd: ParcelFileDescriptor? = null
    private var detachFd: Int? = null
    private var pfdFilePath = false

    private val mpvOptions: Map<String, String>
        get() = buildMap {
            // 视频输出驱动：GPU 渲染（支持 GLSL 滤镜/Anime4K/插帧）
            put("vo", if (Preferences.enableGPUNextRenderer) "gpu-next" else "gpu")
            put(
                "profile",
                when (Preferences.mpvProfile) {
                    "gpu-hq" -> "gpu-hq"
                    "fast" -> "fast"
                    else -> "default"
                },
            )
            put(
                "hwdec",
                when (Preferences.mpvHwdec) {
                    "Auto" -> "auto"
                    "HW" -> "mediacodec-copy"
                    "HW+" -> "mediacodec"
                    "Vulkan" -> "vulkan-copy"
                    "vulkan+" -> "vulkan"
                    "SW" -> "no"
                    else -> "auto"
                },
            )
            put("msg-level", "all=" + if (BuildConfig.DEBUG) "debug" else "warn")
            if (Preferences.mpvInterpolation) {
                put("interpolation", "yes")
                put("tscale", "oversample")
                put("video-sync", "display-resample")
            }
            put("cache", "yes")
            put("cache-secs", Preferences.mpvCacheSecs.toString())
            put("vd-lavc-threads", Runtime.getRuntime().availableProcessors().toString())
            put("framedrop", if (Preferences.mpvFramedrop) "vo" else "no")
            put("deband", if (Preferences.mpvDeband) "yes" else "no")
            put("cache-pause", "no")
            put("network-timeout", Preferences.mpvNetworkTimeout.toString())
            // 指定根证书文件，解决 tls-verify 开启时播放失败
            put("tls-ca-file", AnimeShaders.getCert(context))
            put("tls-verify", if (Preferences.mpvTlsVerify) "no" else "yes")
            // mpv 不走 ProxySelector，HTTP 代理单独喂；不支持 socks
            val proxyIp = Preferences.proxyIp
            val proxyPort = Preferences.proxyPort
            if (proxyIp.isNotBlank() && proxyPort != -1 &&
                Preferences.proxyType == HProxySelector.TYPE_HTTP
            ) {
                put("http-proxy", "http://$proxyIp:$proxyPort")
            }
            put("user-agent", USER_AGENT)
        }

    private val observedProperties = listOf(
        "time-pos" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        "duration" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        "pause" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,
        "paused-for-cache" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,
        "video-params/w" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
        "video-params/h" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
        "demuxer-cache-duration" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
    )

    private fun parseCustomMpvParams(): LinkedHashMap<String, String> {
        val map = linkedMapOf<String, String>()
        Preferences.customMpvParams.split(";").forEach { entry ->
            val trimmed = entry.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
            val parts = trimmed.split(",", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()
                if (key.isNotEmpty() && value.isNotEmpty()) map[key] = value
            }
        }
        return map
    }

    private val mpvEventObserver = object : MPVLib.EventObserver {
        override fun eventProperty(property: String) {}
        override fun eventProperty(property: String, value: String) {}

        override fun eventProperty(property: String, value: Long) {
            when (property) {
                "video-params/w" -> handler.post {
                    if (!released) sizeState = IntSize(value.toInt(), sizeState.height)
                }

                "video-params/h" -> handler.post {
                    if (!released) sizeState = IntSize(sizeState.width, value.toInt())
                }
            }
        }

        override fun eventProperty(property: String, value: Boolean) {
            when (property) {
                "pause" -> handler.post {
                    if (!released) playingState = !value && !endedState
                }

                "paused-for-cache" -> handler.post {
                    if (!released && firstFrameState) bufferingState = value
                }
            }
        }

        override fun eventProperty(property: String, value: Double) {
            when (property) {
                "time-pos" -> handler.post {
                    if (!released) positionMsState = (value * 1_000).toLong()
                }

                "duration" -> handler.post {
                    if (!released) durationMsState = (value * 1_000).toLong()
                }

                "demuxer-cache-duration" -> handler.post {
                    if (!released) cacheDurationMsState = (value * 1_000).toLong()
                }
            }
        }

        override fun event(eventId: Int) {
            handler.post {
                if (released) return@post
                when (eventId) {
                    MPVLib.mpvEventId.MPV_EVENT_START_FILE -> {
                        positionMsState = 0L
                        cacheDurationMsState = 0L
                        durationMsState = 0L
                        endedState = false
                        firstFrameState = false
                        bufferingState = true
                    }

                    MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> {
                        // 暂停态下 PLAYBACK_RESTART 未必到来，这里就收起加载圈
                        bufferingState = false
                        MPVLib.setPropertyDouble("speed", currentSpeed.toDouble())
                        if (pendingSeekMs > 0L) {
                            seekTo(pendingSeekMs)
                            pendingSeekMs = 0L
                        }
                    }

                    MPVLib.mpvEventId.MPV_EVENT_PLAYBACK_RESTART -> {
                        firstFrameState = true
                        bufferingState = false
                    }

                    MPVLib.mpvEventId.MPV_EVENT_END_FILE -> {
                        releaseCurrentPfd()
                        endedState = true
                        playingState = false
                    }
                }
            }
        }
    }

    init {
        // 接管进程级会话：旧实例立即失效（只摘观察者，不做全局拆除）
        owner?.supersede()
        owner = this
        mpvOptions.forEach { (key, value) -> MPVLib.setOptionString(key, value) }
        runCatching {
            parseCustomMpvParams().forEach { (key, value) -> MPVLib.setOptionString(key, value) }
        }.onFailure { Log.w(TAG, "custom mpv params 解析失败", it) }
        observedProperties.forEach { (name, type) -> MPVLib.observeProperty(name, type) }
        MPVLib.addObserver(mpvEventObserver)
    }

    /** 被更新的实例接管：自己出局，但不动全局 vo/surface/文件（它们已归新实例）。 */
    private fun supersede() {
        if (released) return
        released = true
        MPVLib.removeObserver(mpvEventObserver)
        handler.postDelayed({ releaseCurrentPfd() }, 200)
    }

    private fun prepareUri(url: String): String? {
        val uri = url.toUri()
        return when (uri.scheme) {
            "http", "https" -> url
            "file", "content" -> runCatching {
                currentPfd = context.contentResolver.openFileDescriptor(uri, "r")
                detachFd = currentPfd?.detachFd()
                pfdFilePath = true
                detachFd?.let { "fd://$it" }
            }.getOrNull()

            else -> null
        }
    }

    private fun releaseCurrentPfd() {
        if (!pfdFilePath) return
        runCatching { currentPfd?.close() }
        detachFd?.let { fd ->
            runCatching { ParcelFileDescriptor.adoptFd(fd).close() }
        }
        currentPfd = null
        detachFd = null
    }

    override fun load(url: String, startPositionMs: Long) {
        if (released) return
        endedState = false
        firstFrameState = false
        bufferingState = true
        pendingSeekMs = startPositionMs
        MPVLib.setOptionString("force-window", "yes")
        // 进页默认暂停态：loadfile 前先置 pause，加载完停在首帧等用户点播放
        MPVLib.setPropertyBoolean("pause", true)
        val path = prepareUri(url) ?: run {
            Log.e(TAG, "无法解析播放地址: $url")
            return
        }
        MPVLib.command(arrayOf("loadfile", path))
    }

    override fun play() {
        if (released) return
        MPVLib.setPropertyBoolean("pause", false)
    }

    override fun pause() {
        if (released) return
        MPVLib.setPropertyBoolean("pause", true)
    }

    override fun seekTo(positionMs: Long) {
        if (released) return
        endedState = false
        MPVLib.command(
            arrayOf("seek", (positionMs.coerceAtLeast(0L) / 1000.0).toString(), "absolute", "exact")
        )
    }

    override fun setSpeed(speed: Float) {
        currentSpeed = speed
        if (released) return
        MPVLib.setPropertyDouble("speed", speed.toDouble())
    }

    override fun boostSpeed(multiplier: Float) {
        if (released || speedBeforeBoost != null) return
        speedBeforeBoost = currentSpeed
        MPVLib.setPropertyDouble("speed", (currentSpeed * multiplier).toDouble())
    }

    override fun restoreSpeed() {
        if (released) {
            speedBeforeBoost = null
            return
        }
        speedBeforeBoost?.let { MPVLib.setPropertyDouble("speed", it.toDouble()) }
        speedBeforeBoost = null
    }

    override fun setSuperResolution(index: Int) {
        if (released) return
        if (index != 0) {
            MPVLib.command(
                arrayOf("change-list", "glsl-shaders", "set", AnimeShaders.getShader(context, index))
            )
        } else {
            clearSuperResolution()
        }
    }

    private fun clearSuperResolution() {
        MPVLib.command(arrayOf("change-list", "glsl-shaders", "clr", ""))
    }

    override fun release() {
        if (released) {
            // 已被新实例接管：全局会话归它，这里只兜底清自己的 pfd
            releaseCurrentPfd()
            return
        }
        released = true
        MPVLib.removeObserver(mpvEventObserver)
        if (isOwner) {
            owner = null
            clearSuperResolution()
            MPVLib.setPropertyBoolean("pause", true)
            MPVLib.command(arrayOf("loadfile", "", "replace"))
            MPVLib.setPropertyString("vo", "null")
            MPVLib.setOptionString("force-window", "no")
            MPVLib.detachSurface()
        }
        handler.postDelayed({ releaseCurrentPfd() }, 200)
    }

    // ---- Surface 桥（由 VideoSurface 的 SurfaceHolder 回调驱动）----

    fun onSurfaceCreated(surface: Surface) {
        if (released) return
        MPVLib.attachSurface(surface)
        redrawIfPaused()
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        if (released) return
        MPVLib.setPropertyString("android-surface-size", "${width}x${height}")
        redrawIfPaused()
    }

    fun onSurfaceDestroyed() {
        // 只有仍持有会话的实例才允许拆全局 surface——
        // 被接管的旧页在销毁时拆走会把新页的画面弄没
        if (!released && isOwner) {
            MPVLib.detachSurface()
        }
    }

    /**
     * 暂停时 vo 不出新帧，surface 尺寸变化后画面会停留在旧尺寸的最后一帧
     * （切全屏/退全屏时表现为画面残缺；播放中下一帧即自愈）。
     * 用零距离 exact seek 强制 mpv 按新尺寸重绘当前帧。
     */
    private fun redrawIfPaused() {
        if (!playingState && durationMsState > 0L) {
            MPVLib.command(arrayOf("seek", "0", "relative", "exact"))
        }
    }
}
