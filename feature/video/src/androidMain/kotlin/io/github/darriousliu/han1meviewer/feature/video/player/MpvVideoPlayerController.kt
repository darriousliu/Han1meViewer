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
 * MPVLib 是进程级单例（`create/init` 在 HanimeApplication），这里只做
 * per-会话的 option/observer/surface 管理。⚠️ 和 media3 一样不走 OkHttp：
 * mpv 不吃进程级 `ProxySelector`，HTTP 代理要单独喂 `http-proxy` 选项。
 */
@Stable
class MpvVideoPlayerController(
    private val context: Context,
) : VideoPlayerController, SuperResolutionController {

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
    private var released = false

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
                    sizeState = IntSize(value.toInt(), sizeState.height)
                }

                "video-params/h" -> handler.post {
                    sizeState = IntSize(sizeState.width, value.toInt())
                }
            }
        }

        override fun eventProperty(property: String, value: Boolean) {
            when (property) {
                "pause" -> handler.post { playingState = !value && !endedState }
                "paused-for-cache" -> handler.post {
                    if (firstFrameState) bufferingState = value
                }
            }
        }

        override fun eventProperty(property: String, value: Double) {
            when (property) {
                "time-pos" -> handler.post { positionMsState = (value * 1_000).toLong() }
                "duration" -> handler.post { durationMsState = (value * 1_000).toLong() }
                "demuxer-cache-duration" -> handler.post {
                    cacheDurationMsState = (value * 1_000).toLong()
                }
            }
        }

        override fun event(eventId: Int) {
            handler.post {
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
                        if (!released) {
                            endedState = true
                            playingState = false
                        }
                    }
                }
            }
        }
    }

    init {
        mpvOptions.forEach { (key, value) -> MPVLib.setOptionString(key, value) }
        runCatching {
            parseCustomMpvParams().forEach { (key, value) -> MPVLib.setOptionString(key, value) }
        }.onFailure { Log.w(TAG, "custom mpv params 解析失败", it) }
        observedProperties.forEach { (name, type) -> MPVLib.observeProperty(name, type) }
        MPVLib.addObserver(mpvEventObserver)
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
        MPVLib.setPropertyBoolean("pause", false)
    }

    override fun pause() {
        MPVLib.setPropertyBoolean("pause", true)
    }

    override fun seekTo(positionMs: Long) {
        endedState = false
        MPVLib.command(
            arrayOf("seek", (positionMs.coerceAtLeast(0L) / 1000.0).toString(), "absolute", "exact")
        )
    }

    override fun setSpeed(speed: Float) {
        currentSpeed = speed
        MPVLib.setPropertyDouble("speed", speed.toDouble())
    }

    override fun boostSpeed(multiplier: Float) {
        if (speedBeforeBoost != null) return
        speedBeforeBoost = currentSpeed
        MPVLib.setPropertyDouble("speed", (currentSpeed * multiplier).toDouble())
    }

    override fun restoreSpeed() {
        speedBeforeBoost?.let { MPVLib.setPropertyDouble("speed", it.toDouble()) }
        speedBeforeBoost = null
    }

    override fun setSuperResolution(index: Int) {
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
        released = true
        clearSuperResolution()
        MPVLib.setPropertyBoolean("pause", true)
        MPVLib.command(arrayOf("loadfile", "", "replace"))
        MPVLib.setPropertyString("vo", "null")
        MPVLib.setOptionString("force-window", "no")
        MPVLib.detachSurface()
        MPVLib.removeObserver(mpvEventObserver)
        handler.postDelayed({ releaseCurrentPfd() }, 200)
    }

    // ---- Surface 桥（由 VideoSurface 的 SurfaceHolder 回调驱动）----

    fun onSurfaceCreated(surface: Surface) {
        MPVLib.attachSurface(surface)
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        MPVLib.setPropertyString("android-surface-size", "${width}x${height}")
    }

    fun onSurfaceDestroyed() {
        if (!released) {
            MPVLib.detachSurface()
        }
    }
}
