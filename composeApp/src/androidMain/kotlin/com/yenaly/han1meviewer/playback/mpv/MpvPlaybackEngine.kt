package com.yenaly.han1meviewer.playback.mpv

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.Surface
import com.yenaly.han1meviewer.platform.AppBuildInfoProvider
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.USER_AGENT
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.playback.core.PlaybackEngine
import com.yenaly.han1meviewer.playback.core.PlaybackRenderHandle
import com.yenaly.han1meviewer.playback.model.PlaybackCapabilities
import com.yenaly.han1meviewer.playback.model.PlaybackEngineType
import com.yenaly.han1meviewer.playback.model.PlaybackPhase
import com.yenaly.han1meviewer.playback.model.PlaybackSource
import com.yenaly.han1meviewer.playback.model.PlaybackState
import com.yenaly.han1meviewer.playback.model.QualityVariant
import com.yenaly.han1meviewer.util.AnimeShaders
import com.yenaly.han1meviewer.util.AnimeShaders.getCert
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

internal class MpvPlaybackEngine(
    context: Context,
) : PlaybackEngine {
    override val engineType = PlaybackEngineType.Mpv
    override val capabilities = PlaybackCapabilities(supportsSuperResolution = true)

    private val appContext = context.applicationContext
    private val mutableState = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = mutableState.asStateFlow()
    override val renderHandle: PlaybackRenderHandle = MpvPlaybackRenderHandle(this)

    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioFocusRequest: AudioFocusRequest
    private var resumeOnAudioFocusGain = false
    private var isDucked = false

    private var currentSurface: Surface? = null
    private val fileDescriptorOwner = MpvFileDescriptorOwner(::closeFileDescriptor)
    private val loadSession = MpvLoadSession()
    private val progressPublishGate = MpvProgressPublishGate()
    private var loadTimeoutRunnable: Runnable? = null
    @Volatile
    private var isReleased = false
    @Volatile
    private var highFrequencyProgressUpdates = false
    private var pendingPlayback = MpvPendingPlayback()
    private var runtimeState = MpvRuntimeState()
    @Volatile
    private var durationMs = 0L
    @Volatile
    private var positionMs = 0L
    @Volatile
    private var cacheDurationMs = 0L

    private val progressPublishRunnable = Runnable {
        if (progressPublishGate.consumeScheduled() &&
            !isReleased &&
            !loadSession.isPending
        ) {
            publishProgressState()
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                restoreVolumeAfterDuck()
                if (resumeOnAudioFocusGain) {
                    resumeOnAudioFocusGain = false
                    playWithoutRequestingFocus()
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> duckVolume()

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                resumeOnAudioFocusGain = mutableState.value.isPlaying || mutableState.value.playWhenReady
                pauseWithoutAbandoningFocus()
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnAudioFocusGain = false
                pauseWithoutAbandoningFocus()
                abandonAudioFocus()
            }
        }
    }

    private val eventObserver = object : MPVLib.EventObserver {
        override fun eventProperty(property: String) = Unit

        override fun eventProperty(property: String, value: Long) = postMpvCallback {
            when (property) {
                "video-params/w" -> mutableState.update { it.copy(videoWidth = value.toInt()) }
                "video-params/h" -> mutableState.update { it.copy(videoHeight = value.toInt()) }
            }
        }

        override fun eventProperty(property: String, value: Boolean) = postMpvCallback {
            if (property == "pause" || property == "playback-active") {
                runtimeState = runtimeState.withBooleanProperty(property, value)
                publishPlayingState()
            }
        }

        override fun eventProperty(property: String, value: String) = Unit

        override fun eventProperty(property: String, value: Double) {
            if (isReleased) return
            when (property) {
                "time-pos" -> positionMs = mpvSecondsToMillis(value)
                "duration" -> durationMs = mpvSecondsToMillis(value)
                "demuxer-cache-duration" -> cacheDurationMs = mpvSecondsToMillis(value)
            }
            scheduleProgressPublish()
        }

        override fun event(eventId: Int) {
            val actualPath = if (eventId in LOAD_SCOPED_EVENTS) currentMpvPath() else null
            val loadEvent = when (eventId) {
                MPVLib.mpvEventId.MPV_EVENT_START_FILE ->
                    loadSession.onNativeStart(actualPath)

                MPVLib.mpvEventId.MPV_EVENT_END_FILE ->
                    loadSession.onNativeEnd(actualPath)

                MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED,
                MPVLib.mpvEventId.MPV_EVENT_PLAYBACK_RESTART ->
                    loadSession.snapshotNativeEvent(actualPath)

                else -> null
            }
            if (eventId == MPVLib.mpvEventId.MPV_EVENT_START_FILE &&
                loadEvent != null &&
                loadSession.isPendingEvent(loadEvent)
            ) {
                // Reset on the native callback thread so later progress events cannot be erased by
                // a delayed main-thread START_FILE handler.
                positionMs = 0L
                durationMs = 0L
                cacheDurationMs = 0L
                cancelProgressPublish()
            }
            postMpvCallback {
                when (eventId) {
                    MPVLib.mpvEventId.MPV_EVENT_START_FILE -> {
                        if (loadEvent == null || !loadSession.isPendingEvent(loadEvent)) {
                            return@postMpvCallback
                        }
                        runtimeState = runtimeState.copy(playbackActive = false)
                        mutableState.update {
                            it.copy(
                                phase = PlaybackPhase.Preparing,
                                isPlaying = false,
                                errorMessage = null,
                            )
                        }
                    }

                    MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED -> onFileLoaded(loadEvent)

                    MPVLib.mpvEventId.MPV_EVENT_PLAYBACK_RESTART -> {
                        if (loadEvent == null || !loadSession.isLoadedEvent(loadEvent)) {
                            return@postMpvCallback
                        }
                        runtimeState = runtimeState.copy(playbackActive = true)
                        mutableState.update {
                            it.copy(
                                phase = PlaybackPhase.Ready,
                                errorMessage = null,
                            )
                        }
                        publishPlayingState()
                    }

                    MPVLib.mpvEventId.MPV_EVENT_END_FILE -> {
                        when {
                            loadEvent == null -> Unit
                            loadSession.consumePendingEnd(loadEvent) -> {
                                publishError("MPV could not load the selected video.")
                            }
                            !loadSession.isPending && loadSession.consumeLoadedEnd(loadEvent) -> {
                                publishProgressStateNow()
                                runtimeState = runtimeState.copy(
                                    playbackActive = false,
                                    paused = true,
                                )
                                fileDescriptorOwner.onPlaybackEnded()
                                abandonAudioFocus()
                                mutableState.update { it.reduceMpvEnded() }
                            }
                        }
                    }

                    MPVLib.mpvEventId.MPV_EVENT_SHUTDOWN -> {
                        if (!isReleased) publishError("MPV playback process shut down.")
                    }
                }
            }
        }
    }

    init {
        ensureMpvInitialized(appContext)
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            .setOnAudioFocusChangeListener(audioFocusChangeListener, mainHandler)
            .setWillPauseWhenDucked(false)
            .build()
        applyMpvOptions()
        OBSERVED_PROPERTIES.forEach { (name, format) -> MPVLib.observeProperty(name, format) }
        MPVLib.addObserver(eventObserver)
    }

    override fun load(
        source: PlaybackSource,
        qualityId: String?,
        startPositionMs: Long,
        playWhenReady: Boolean,
    ) {
        if (isReleased) return
        val quality = source.resolveQuality(qualityId)
        mutableState.value = PlaybackState(
            source = source,
            phase = PlaybackPhase.Preparing,
            playWhenReady = playWhenReady,
            positionMs = startPositionMs,
            speed = mutableState.value.speed,
            selectedQualityId = quality.id,
        )
        startLoad(
            source = source,
            quality = quality,
            positionMs = startPositionMs,
            playWhenReady = playWhenReady,
        )
    }

    override fun play() {
        if (isReleased || mutableState.value.source == null) return
        if (loadSession.isPending) {
            pendingPlayback = pendingPlayback.copy(playWhenReady = true)
            mutableState.update { it.copy(playWhenReady = true) }
            return
        }
        if (requestAudioFocus()) playWithoutRequestingFocus()
    }

    override fun pause() {
        if (isReleased) return
        resumeOnAudioFocusGain = false
        pauseWithoutAbandoningFocus()
        abandonAudioFocus()
    }

    override fun seekTo(positionMs: Long) {
        if (isReleased || mutableState.value.source == null) return
        val duration = mutableState.value.durationMs
        val target = if (duration > 0L) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        if (loadSession.isPending) pendingPlayback = pendingPlayback.seekTo(target)
        cancelProgressPublish()
        runMpvCommand {
            MPVLib.command(arrayOf("seek", (target / 1_000.0).toString(), "absolute", "exact"))
        }
        this.positionMs = target
        mutableState.update { it.copy(positionMs = target, errorMessage = null) }
    }

    override fun setSpeed(speed: Float) {
        if (isReleased) return
        runMpvCommand { MPVLib.setPropertyDouble("speed", speed.toDouble()) }
        mutableState.update { it.copy(speed = speed) }
    }

    override fun setHighFrequencyProgressUpdates(enabled: Boolean) {
        if (isReleased || highFrequencyProgressUpdates == enabled) return
        highFrequencyProgressUpdates = enabled
        if (progressPublishGate.cancel()) {
            mainHandler.removeCallbacks(progressPublishRunnable)
            scheduleProgressPublish()
        }
    }

    override fun selectQuality(qualityId: String) {
        if (isReleased || qualityId == mutableState.value.selectedQualityId) return
        val source = mutableState.value.source ?: return
        val quality = source.qualities.firstOrNull { it.id == qualityId } ?: return
        val position = (if (loadSession.isPending) pendingPlayback.positionMs else positionMs)
            .coerceAtLeast(0L)
        val shouldPlay = mutableState.value.playWhenReady || mutableState.value.isPlaying
        mutableState.update {
            it.copy(
                phase = PlaybackPhase.Preparing,
                isPlaying = false,
                positionMs = position,
                bufferedPositionMs = 0L,
                bufferedPercentage = 0,
                selectedQualityId = quality.id,
                errorMessage = null,
            )
        }
        startLoad(source, quality, position, shouldPlay)
    }

    override fun setSuperResolution(index: Int) {
        if (isReleased) return
        runMpvCommand {
            if (index == 0) {
                clearSuperResolution()
            } else {
                MPVLib.command(
                    arrayOf(
                        "change-list",
                        "glsl-shaders",
                        "set",
                        AnimeShaders.getShader(appContext, index),
                    )
                )
            }
        }
    }

    override fun release() {
        if (isReleased) return
        isReleased = true
        cancelLoadTimeout()
        cancelProgressPublish()
        loadSession.release()
        resumeOnAudioFocusGain = false
        abandonAudioFocus()
        runCatching { clearSuperResolution() }
        runCatching { MPVLib.setPropertyBoolean("pause", true) }
        runCatching { MPVLib.command(arrayOf("loadfile", "", "replace")) }
        runCatching { MPVLib.setPropertyString("vo", "null") }
        runCatching { MPVLib.setOptionString("force-window", "no") }
        runCatching { MPVLib.detachSurface() }
        currentSurface = null
        runCatching { MPVLib.removeObserver(eventObserver) }
        fileDescriptorOwner.release()
        mainHandler.removeCallbacksAndMessages(null)
        mutableState.value = PlaybackState()
    }

    internal fun attachSurface(surface: Surface) {
        if (isReleased) return
        currentSurface = surface
        runMpvCommand { MPVLib.attachSurface(surface) }
    }

    internal fun detachSurface(surface: Surface) {
        if (currentSurface !== surface) return
        runMpvCommand { MPVLib.detachSurface() }
        currentSurface = null
    }

    internal fun updateSurfaceSize(width: Int, height: Int) {
        if (isReleased || width <= 0 || height <= 0) return
        runMpvCommand { MPVLib.setPropertyString("android-surface-size", "${width}x$height") }
    }

    private fun startLoad(
        source: PlaybackSource,
        quality: QualityVariant,
        positionMs: Long,
        playWhenReady: Boolean,
    ) {
        cancelProgressPublish()
        val preparedUri = prepareUri(quality.uri) ?: run {
            publishError("Unsupported or inaccessible media URI: ${quality.uri}")
            return
        }
        fileDescriptorOwner.replaceWith(preparedUri.fileDescriptor)
        pendingPlayback = MpvPendingPlayback(
            positionMs = positionMs.coerceAtLeast(0L),
            playWhenReady = playWhenReady,
        )
        if (!applyHttpHeaders(source.headersFor(quality))) return
        val loadTicket = loadSession.begin(preparedUri.value)
        scheduleLoadTimeout(loadTicket)
        if (!runMpvCommand {
            MPVLib.setOptionString("force-window", "yes")
            MPVLib.command(arrayOf("loadfile", preparedUri.value, "replace"))
            currentSurface?.let(MPVLib::attachSurface)
        }) {
            loadSession.discardUnsubmitted(loadTicket)
        }
    }

    private fun onFileLoaded(loadEvent: MpvNativeLoadEvent?) {
        if (loadEvent == null || !loadSession.complete(loadEvent)) return
        cancelLoadTimeout()
        fileDescriptorOwner.onFileLoaded()
        val speed = mutableState.value.speed
        if (!runMpvCommand {
            MPVLib.setPropertyDouble("speed", speed.toDouble())
            if (pendingPlayback.positionMs > 0L) {
                MPVLib.command(
                    arrayOf(
                        "seek",
                        (pendingPlayback.positionMs / 1_000.0).toString(),
                        "absolute",
                        "exact",
                    )
                )
            }
        }) return
        val shouldPlay = pendingPlayback.playWhenReady && requestAudioFocus()
        runtimeState = runtimeState.copy(paused = !shouldPlay)
        if (!runMpvCommand { MPVLib.setPropertyBoolean("pause", !shouldPlay) }) return
        positionMs = pendingPlayback.positionMs
        mutableState.update {
            it.copy(
                phase = PlaybackPhase.Ready,
                playWhenReady = shouldPlay,
                positionMs = pendingPlayback.positionMs,
                speed = speed,
                errorMessage = null,
            )
        }
        publishProgressStateNow()
        publishPlayingState()
    }

    private fun prepareUri(rawUri: String): PreparedUri? {
        val uri = Uri.parse(rawUri)
        return when (uri.scheme?.lowercase()) {
            "http", "https" -> PreparedUri(value = rawUri)
            "file", "content", "android.resource" -> openFileDescriptor(uri)
            null -> File(rawUri).takeIf(File::exists)?.let { PreparedUri(value = it.absolutePath) }
            else -> null
        }
    }

    private fun openFileDescriptor(uri: Uri): PreparedUri? = runCatching {
        val parcelFileDescriptor = appContext.contentResolver.openFileDescriptor(uri, "r")
            ?: return@runCatching null
        val detached = parcelFileDescriptor.detachFd()
        parcelFileDescriptor.close()
        PreparedUri(value = "fd://$detached", fileDescriptor = detached)
    }.onFailure { error ->
        Log.e(TAG, "Unable to open local media URI: $uri", error)
    }.getOrNull()

    private fun applyMpvOptions() {
        buildMap {
            put("vo", if (Preferences.enableGPUNextRenderer) "gpu-next" else "gpu")
            put(
                "profile",
                when (Preferences.mpvProfile) {
                    "gpu-hq" -> "gpu-hq"
                    "fast" -> "fast"
                    else -> "default"
                }
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
                }
            )
            put(
                "msg-level",
                "all=${if (AppBuildInfoProvider.current.debug) "debug" else "warn"}",
            )
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
            put("tls-ca-file", getCert(appContext))
            // This preference means "ignore certificate verification" despite its legacy name.
            put("tls-verify", if (Preferences.mpvTlsVerify) "no" else "yes")
            val proxyIp = Preferences.proxyIp
            val proxyPort = Preferences.proxyPort
            if (proxyIp.isNotBlank() && proxyPort != -1 &&
                Preferences.proxyType == HProxySelector.TYPE_HTTP
            ) {
                put("http-proxy", "http://$proxyIp:$proxyPort")
            }
            put("user-agent", USER_AGENT)
        }.forEach { (key, value) -> MPVLib.setOptionString(key, value) }

        parseMpvCustomParams(Preferences.customMpvParams).forEach { (key, value) ->
            MPVLib.setOptionString(key, value)
        }
    }

    private fun applyHttpHeaders(headers: Map<String, String>): Boolean {
        val value = headers.entries.joinToString(",") { (key, headerValue) -> "$key: $headerValue" }
        return runMpvCommand { MPVLib.setPropertyString("http-header-fields", value) }
    }

    private fun playWithoutRequestingFocus() {
        if (loadSession.isPending) {
            pendingPlayback = pendingPlayback.copy(playWhenReady = true)
            mutableState.update { it.copy(playWhenReady = true) }
            return
        }
        runtimeState = runtimeState.copy(paused = false)
        runMpvCommand { MPVLib.setPropertyBoolean("pause", false) }
        mutableState.update { it.copy(playWhenReady = true) }
        publishPlayingState()
    }

    private fun pauseWithoutAbandoningFocus() {
        pendingPlayback = pendingPlayback.copy(playWhenReady = false)
        runtimeState = runtimeState.copy(paused = true)
        runMpvCommand { MPVLib.setPropertyBoolean("pause", true) }
        mutableState.update { it.copy(isPlaying = false, playWhenReady = false) }
    }

    private fun requestAudioFocus(): Boolean =
        runCatching {
            audioManager.requestAudioFocus(audioFocusRequest) ==
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }.getOrElse { error ->
            Log.e(TAG, "Unable to acquire audio focus.", error)
            false
        }

    private fun abandonAudioFocus() {
        runCatching { audioManager.abandonAudioFocusRequest(audioFocusRequest) }
            .onFailure { Log.w(TAG, "Unable to abandon audio focus.", it) }
        restoreVolumeAfterDuck()
    }

    private fun duckVolume() {
        if (isDucked) return
        isDucked = true
        runMpvCommand { MPVLib.setPropertyDouble("volume", DUCK_VOLUME) }
    }

    private fun restoreVolumeAfterDuck() {
        if (!isDucked) return
        isDucked = false
        runCatching { MPVLib.setPropertyDouble("volume", NORMAL_VOLUME) }
            .onFailure { Log.w(TAG, "Unable to restore MPV volume after ducking.", it) }
    }

    private fun publishPlayingState() {
        mutableState.update { it.reduceMpvRuntime(runtimeState) }
    }

    private fun publishProgressState() {
        if (mutableState.value.phase == PlaybackPhase.Ended ||
            mutableState.value.phase == PlaybackPhase.Error
        ) {
            return
        }
        val bufferedPosition = mpvBufferedPositionMs(positionMs, cacheDurationMs, durationMs)
        val bufferedPercentage = if (durationMs > 0L) {
            (bufferedPosition * 100L / durationMs).toInt().coerceIn(0, 100)
        } else {
            0
        }
        mutableState.update {
            it.copy(
                positionMs = positionMs,
                durationMs = durationMs,
                bufferedPositionMs = bufferedPosition,
                bufferedPercentage = bufferedPercentage,
            )
        }
    }

    private fun scheduleProgressPublish() {
        if (isReleased || loadSession.isPending ||
            mutableState.value.phase == PlaybackPhase.Ended ||
            mutableState.value.phase == PlaybackPhase.Error ||
            !progressPublishGate.requestSchedule()
        ) {
            return
        }
        val interval = mpvProgressPublishIntervalMs(highFrequencyProgressUpdates)
        mainHandler.postDelayed(progressPublishRunnable, interval)
    }

    private fun publishProgressStateNow() {
        cancelProgressPublish()
        if (!isReleased) publishProgressState()
    }

    private fun cancelProgressPublish() {
        if (progressPublishGate.cancel()) {
            mainHandler.removeCallbacks(progressPublishRunnable)
        }
    }

    private fun publishError(message: String, cause: Throwable? = null) {
        cause?.let { Log.e(TAG, message, it) } ?: Log.e(TAG, message)
        cancelLoadTimeout()
        publishProgressStateNow()
        loadSession.cancel()
        runtimeState = runtimeState.copy(playbackActive = false, paused = true)
        fileDescriptorOwner.onPlaybackEnded()
        abandonAudioFocus()
        mutableState.update { it.reduceMpvError(message) }
    }

    private inline fun runMpvCommand(action: () -> Unit): Boolean {
        if (isReleased) return false
        return try {
            action()
            true
        } catch (error: Exception) {
            publishError(error.localizedMessage ?: "MPV command failed.", error)
            false
        }
    }

    private fun clearSuperResolution() {
        MPVLib.command(arrayOf("change-list", "glsl-shaders", "clr", ""))
    }

    private fun postMpvCallback(action: () -> Unit) {
        mainHandler.post {
            if (!isReleased) action()
        }
    }

    private fun currentMpvPath(): String? =
        runCatching { MPVLib.getPropertyString("path") }
            .onFailure { Log.w(TAG, "Unable to read MPV media path.", it) }
            .getOrNull()

    private fun scheduleLoadTimeout(ticket: MpvLoadTicket) {
        cancelLoadTimeout()
        val timeout = Runnable {
            loadTimeoutRunnable = null
            if (!isReleased && loadSession.expire(ticket)) {
                publishError("MPV did not finish loading the selected video in time.")
            }
        }
        loadTimeoutRunnable = timeout
        mainHandler.postDelayed(timeout, mpvLoadTimeoutMs(Preferences.mpvNetworkTimeout))
    }

    private fun cancelLoadTimeout() {
        loadTimeoutRunnable?.let(mainHandler::removeCallbacks)
        loadTimeoutRunnable = null
    }

    private fun closeFileDescriptor(fileDescriptor: Int) {
        runCatching { ParcelFileDescriptor.adoptFd(fileDescriptor).close() }
            .onFailure { Log.w(TAG, "Unable to close media file descriptor $fileDescriptor", it) }
    }

    private data class PreparedUri(
        val value: String,
        val fileDescriptor: Int? = null,
    )

    private companion object {
        const val TAG = "MpvPlaybackEngine"
        const val DUCK_VOLUME = 20.0
        const val NORMAL_VOLUME = 100.0
        val LOAD_SCOPED_EVENTS = setOf(
            MPVLib.mpvEventId.MPV_EVENT_START_FILE,
            MPVLib.mpvEventId.MPV_EVENT_FILE_LOADED,
            MPVLib.mpvEventId.MPV_EVENT_PLAYBACK_RESTART,
            MPVLib.mpvEventId.MPV_EVENT_END_FILE,
        )

        val OBSERVED_PROPERTIES = listOf(
            "time-pos" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
            "duration" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
            "pause" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,
            "playback-active" to MPVLib.mpvFormat.MPV_FORMAT_FLAG,
            "video-params/w" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
            "video-params/h" to MPVLib.mpvFormat.MPV_FORMAT_INT64,
            "demuxer-cache-duration" to MPVLib.mpvFormat.MPV_FORMAT_DOUBLE,
        )

        private val initializationLock = Any()

        @Volatile
        private var isMpvInitialized = false

        fun ensureMpvInitialized(context: Context) {
            if (isMpvInitialized) return
            synchronized(initializationLock) {
                if (isMpvInitialized) return
                MPVLib.create(context.applicationContext)
                MPVLib.init()
                isMpvInitialized = true
            }
        }
    }
}

internal class MpvPlaybackRenderHandle(
    private val engine: MpvPlaybackEngine,
) : PlaybackRenderHandle {
    override fun attachSurface(surface: Surface) = engine.attachSurface(surface)

    override fun detachSurface(surface: Surface) = engine.detachSurface(surface)

    override fun updateSurfaceSize(width: Int, height: Int) =
        engine.updateSurfaceSize(width, height)
}

internal fun parseMpvCustomParams(rawInput: String): Map<String, String> = buildMap {
    rawInput.split(';').forEach { entry ->
        val trimmedEntry = entry.trim()
        if (trimmedEntry.isEmpty() || trimmedEntry.startsWith('#')) return@forEach
        val parts = trimmedEntry.split(',', limit = 2)
        if (parts.size != 2) return@forEach
        val key = parts[0].trim()
        val value = parts[1].trim()
        if (key.isNotEmpty() && value.isNotEmpty()) put(key, value)
    }
}
