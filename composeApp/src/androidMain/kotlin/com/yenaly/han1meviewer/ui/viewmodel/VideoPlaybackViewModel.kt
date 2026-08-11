package com.yenaly.han1meviewer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.playback.core.PlaybackController
import com.yenaly.han1meviewer.playback.core.PlaybackControllerFactory
import com.yenaly.han1meviewer.playback.model.PlaybackEngineType
import com.yenaly.han1meviewer.playback.model.PlaybackPhase
import com.yenaly.han1meviewer.playback.model.PlaybackSource

/** Route-scoped owner of the player instance and its lifecycle. */
class VideoPlaybackViewModel(application: Application) : AndroidViewModel(application) {
    val controller: PlaybackController = PlaybackControllerFactory.create(
        context = application,
        engineType = PlaybackEngineType.fromString(Preferences.switchPlayerKernel),
    )
    val state = controller.state
    val capabilities = controller.capabilities

    private var lastLoad: LoadRequest? = null

    init {
        controller.setSpeed(Preferences.playerSpeed)
    }

    fun load(
        source: PlaybackSource,
        qualityId: String? = null,
        startPositionMs: Long = 0L,
        playWhenReady: Boolean = false,
    ) {
        val request = LoadRequest(
            source = source,
            qualityId = source.resolveQuality(qualityId).id,
            startPositionMs = startPositionMs.coerceAtLeast(0L),
            playWhenReady = playWhenReady,
        )
        val speed = state.value.speed
        lastLoad = request
        controller.load(
            source = request.source,
            qualityId = request.qualityId,
            startPositionMs = request.startPositionMs,
            playWhenReady = request.playWhenReady,
        )
        controller.setSpeed(speed)
    }

    fun play() {
        if (state.value.phase == PlaybackPhase.Ended) controller.seekTo(0L)
        controller.play()
    }

    fun pause() = controller.pause()

    fun togglePlayPause() = controller.togglePlayPause()

    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)

    fun selectQuality(qualityId: String) = controller.selectQuality(qualityId)

    fun setSpeed(speed: Float) = controller.setSpeed(speed)

    fun setHighFrequencyProgressUpdates(enabled: Boolean) =
        controller.setHighFrequencyProgressUpdates(enabled)

    fun setSuperResolution(index: Int) = controller.setSuperResolution(index)

    fun retry() {
        val previous = lastLoad ?: return
        val playback = state.value
        val request = previous.copy(
            qualityId = playback.selectedQualityId ?: previous.qualityId,
            startPositionMs = playback.positionMs,
            playWhenReady = true,
        )
        lastLoad = request
        controller.load(
            source = request.source,
            qualityId = request.qualityId,
            startPositionMs = request.startPositionMs,
            playWhenReady = request.playWhenReady,
        )
        controller.setSpeed(playback.speed)
    }

    fun replay() {
        controller.seekTo(0L)
        controller.play()
    }

    override fun onCleared() {
        controller.release()
        super.onCleared()
    }

    private data class LoadRequest(
        val source: PlaybackSource,
        val qualityId: String,
        val startPositionMs: Long,
        val playWhenReady: Boolean,
    )
}
