package com.yenaly.han1meviewer.playback.media3

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.yenaly.han1meviewer.USER_AGENT
import com.yenaly.han1meviewer.playback.core.PlaybackEngine
import com.yenaly.han1meviewer.playback.core.PlaybackRenderHandle
import com.yenaly.han1meviewer.playback.model.PlaybackCapabilities
import com.yenaly.han1meviewer.playback.model.PlaybackEngineType
import com.yenaly.han1meviewer.playback.model.PlaybackPhase
import com.yenaly.han1meviewer.playback.model.PlaybackSource
import com.yenaly.han1meviewer.playback.model.PlaybackState
import com.yenaly.han1meviewer.playback.model.QualityVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@OptIn(UnstableApi::class)
internal class Media3PlaybackEngine(
    context: Context,
) : PlaybackEngine, Player.Listener {
    override val engineType = PlaybackEngineType.Media3
    override val capabilities = PlaybackCapabilities()

    private val mutableState = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = mutableState.asStateFlow()

    private val appContext = context.applicationContext
    internal val player: ExoPlayer
    override val renderHandle: PlaybackRenderHandle

    private val playerHandler: Handler
    private var isReleased = false
    private var highFrequencyProgressUpdates = false

    private val progressUpdater = object : Runnable {
        override fun run() {
            if (isReleased) return
            updateProgressSnapshot()
            if (mutableState.value.source != null &&
                mutableState.value.phase !in setOf(PlaybackPhase.Ended, PlaybackPhase.Error)
            ) {
                playerHandler.postDelayed(this, progressUpdateIntervalMs)
            }
        }
    }

    init {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "Media3PlaybackEngine must be created on the main thread."
        }
        val playerBuilder = if (Util.isRunningOnEmulator()) {
            ExoPlayer.Builder(
                appContext,
                DefaultRenderersFactory(appContext)
                    .setMediaCodecSelector(MediaCodecSelector.PREFER_SOFTWARE),
            )
        } else {
            ExoPlayer.Builder(appContext)
        }
        player = playerBuilder
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
                setHandleAudioBecomingNoisy(true)
                addListener(this@Media3PlaybackEngine)
            }
        playerHandler = Handler(player.applicationLooper)
        renderHandle = Media3PlaybackRenderHandle(this)
    }

    override fun load(
        source: PlaybackSource,
        qualityId: String?,
        startPositionMs: Long,
        playWhenReady: Boolean,
    ) = runOnPlayerThread {
        if (isReleased) return@runOnPlayerThread
        val quality = source.resolveQuality(qualityId)
        mutableState.value = PlaybackState(
            source = source,
            phase = PlaybackPhase.Preparing,
            playWhenReady = playWhenReady,
            positionMs = startPositionMs,
            speed = mutableState.value.speed,
            selectedQualityId = quality.id,
        )
        player.playbackParameters = PlaybackParameters(mutableState.value.speed)
        player.setMediaSource(
            createMediaSource(source, quality),
            startPositionMs.coerceAtLeast(0L),
        )
        player.prepare()
        player.playWhenReady = playWhenReady
        restartProgressUpdates()
    }

    override fun play() = runOnPlayerThread {
        if (isReleased || mutableState.value.source == null) return@runOnPlayerThread
        if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)
        player.play()
    }

    override fun pause() = runOnPlayerThread {
        if (!isReleased) player.pause()
    }

    override fun seekTo(positionMs: Long) = runOnPlayerThread {
        if (isReleased || mutableState.value.source == null) return@runOnPlayerThread
        val duration = player.duration.validDurationOrZero()
        val target = if (duration > 0L) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        player.seekTo(target)
        mutableState.update { it.copy(positionMs = target, errorMessage = null) }
        restartProgressUpdates()
    }

    override fun setSpeed(speed: Float) = runOnPlayerThread {
        if (isReleased) return@runOnPlayerThread
        player.playbackParameters = PlaybackParameters(speed)
        mutableState.update { it.copy(speed = speed) }
    }

    override fun setHighFrequencyProgressUpdates(enabled: Boolean) = runOnPlayerThread {
        if (isReleased || highFrequencyProgressUpdates == enabled) return@runOnPlayerThread
        highFrequencyProgressUpdates = enabled
        if (mutableState.value.source != null) restartProgressUpdates()
    }

    override fun selectQuality(qualityId: String) = runOnPlayerThread {
        if (isReleased || qualityId == mutableState.value.selectedQualityId) return@runOnPlayerThread
        val source = mutableState.value.source ?: return@runOnPlayerThread
        val quality = source.qualities.firstOrNull { it.id == qualityId } ?: return@runOnPlayerThread
        val snapshot = Media3ContinuitySnapshot(
            positionMs = player.currentPosition,
            playWhenReady = mutableState.value.isPlaying || mutableState.value.playWhenReady,
            speed = player.playbackParameters.speed,
        )

        mutableState.update { it.prepareMedia3QualitySwitch(quality.id, snapshot) }
        player.setMediaSource(createMediaSource(source, quality), snapshot.positionMs.coerceAtLeast(0L))
        player.prepare()
        player.playbackParameters = PlaybackParameters(snapshot.speed)
        player.playWhenReady = snapshot.playWhenReady
        restartProgressUpdates()
    }

    override fun setSuperResolution(index: Int) = Unit

    override fun release() = runOnPlayerThread {
        if (isReleased) return@runOnPlayerThread
        isReleased = true
        playerHandler.removeCallbacks(progressUpdater)
        player.removeListener(this)
        player.clearVideoSurface()
        player.release()
        mutableState.value = PlaybackState()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_IDLE &&
            mutableState.value.phase == PlaybackPhase.Error
        ) {
            updateProgressSnapshot()
            return
        }
        val phase = when (playbackState) {
            Player.STATE_IDLE -> {
                if (mutableState.value.source == null) PlaybackPhase.Idle else PlaybackPhase.Preparing
            }

            Player.STATE_BUFFERING -> PlaybackPhase.Buffering
            Player.STATE_READY -> PlaybackPhase.Ready
            Player.STATE_ENDED -> PlaybackPhase.Ended
            else -> return
        }
        mutableState.update {
            it.reduceMedia3Phase(
                phase = phase,
                isPlaying = player.isPlaying,
                playWhenReady = player.playWhenReady,
            )
        }
        updateProgressSnapshot()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        mutableState.update {
            if (it.phase == PlaybackPhase.Error || it.phase == PlaybackPhase.Ended) {
                it.copy(isPlaying = false, playWhenReady = false)
            } else {
                it.copy(
                    isPlaying = isPlaying,
                    playWhenReady = player.playWhenReady,
                )
            }
        }
        if (isPlaying) restartProgressUpdates() else updateProgressSnapshot()
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        mutableState.update {
            if (it.phase == PlaybackPhase.Error || it.phase == PlaybackPhase.Ended) {
                it
            } else {
                it.copy(playWhenReady = playWhenReady)
            }
        }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        mutableState.update { it.copy(speed = playbackParameters.speed) }
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        mutableState.update {
            it.copy(
                videoWidth = (videoSize.width * videoSize.pixelWidthHeightRatio).toInt(),
                videoHeight = videoSize.height,
            )
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        mutableState.update {
            it.reduceMedia3Error(error.localizedMessage ?: error.errorCodeName)
        }
        playerHandler.removeCallbacks(progressUpdater)
    }

    override fun onEvents(player: Player, events: Player.Events) {
        updateProgressSnapshot()
    }

    internal fun runOnPlayerThread(action: () -> Unit) {
        if (Looper.myLooper() == player.applicationLooper) {
            action()
        } else {
            playerHandler.post(action)
        }
    }

    private fun restartProgressUpdates() {
        playerHandler.removeCallbacks(progressUpdater)
        playerHandler.post(progressUpdater)
    }

    private fun updateProgressSnapshot() {
        if (isReleased) return
        mutableState.update {
            it.copy(
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.validDurationOrZero(),
                bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
                bufferedPercentage = player.bufferedPercentage.coerceIn(0, 100),
            )
        }
    }

    private val progressUpdateIntervalMs: Long
        get() = if (highFrequencyProgressUpdates) {
            HIGH_FREQUENCY_PROGRESS_UPDATE_INTERVAL_MS
        } else {
            PROGRESS_UPDATE_INTERVAL_MS
        }

    private fun createMediaSource(
        source: PlaybackSource,
        quality: QualityVariant,
    ) = DefaultMediaSourceFactory(
        DefaultDataSource.Factory(
            appContext,
            DefaultHttpDataSource.Factory()
                .setUserAgent(USER_AGENT)
                .setDefaultRequestProperties(source.headersFor(quality)),
        )
    ).createMediaSource(
        MediaItem.Builder()
            .setMediaId("${source.id}:${quality.id}")
            .setUri(quality.uri)
            .setMimeType(quality.mimeType.toMedia3MimeType())
            .build()
    )

    private companion object {
        const val PROGRESS_UPDATE_INTERVAL_MS = 250L
        const val HIGH_FREQUENCY_PROGRESS_UPDATE_INTERVAL_MS = 100L
    }
}

internal class Media3PlaybackRenderHandle(
    private val engine: Media3PlaybackEngine,
) : PlaybackRenderHandle {
    internal val player: Player
        get() = engine.player

    override fun attachSurface(surface: Surface) = engine.runOnPlayerThread {
        engine.player.setVideoSurface(surface)
    }

    override fun detachSurface(surface: Surface) = engine.runOnPlayerThread {
        engine.player.clearVideoSurface(surface)
    }

    override fun updateSurfaceSize(width: Int, height: Int) = Unit
}

private fun Long.validDurationOrZero(): Long =
    takeUnless { it == C.TIME_UNSET || it < 0L } ?: 0L

private fun String?.toMedia3MimeType(): String? {
    val value = this?.trim()?.takeIf(String::isNotEmpty) ?: return null
    return when {
        value.contains("mpegurl", ignoreCase = true) ||
                value.contains("m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8

        '/' in value -> value
        else -> "video/$value"
    }
}
