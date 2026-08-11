package com.yenaly.han1meviewer.playback.mpv

import com.yenaly.han1meviewer.playback.model.PlaybackPhase
import com.yenaly.han1meviewer.playback.model.PlaybackState
import java.util.ArrayDeque

internal fun mpvLoadTimeoutMs(networkTimeoutSeconds: Int): Long =
    networkTimeoutSeconds.coerceIn(5, 60) * 1_000L + 10_000L

internal fun mpvProgressPublishIntervalMs(highFrequency: Boolean): Long =
    if (highFrequency) 100L else 250L

internal fun mpvSecondsToMillis(seconds: Double): Long = when {
    !seconds.isFinite() || seconds <= 0.0 -> 0L
    seconds >= Long.MAX_VALUE / 1_000.0 -> Long.MAX_VALUE
    else -> (seconds * 1_000.0).toLong()
}

internal fun mpvBufferedPositionMs(
    positionMs: Long,
    cacheDurationMs: Long,
    durationMs: Long,
): Long {
    if (durationMs <= 0L) return 0L
    val position = positionMs.coerceAtLeast(0L)
    val cache = cacheDurationMs.coerceAtLeast(0L)
    val buffered = if (cache > Long.MAX_VALUE - position) Long.MAX_VALUE else position + cache
    return buffered.coerceAtMost(durationMs)
}

internal data class MpvPendingPlayback(
    val positionMs: Long = 0L,
    val playWhenReady: Boolean = false,
) {
    fun seekTo(positionMs: Long): MpvPendingPlayback =
        copy(positionMs = positionMs.coerceAtLeast(0L))
}

/** Pure MPV runtime flags used to reduce native property callbacks into UI state. */
internal data class MpvRuntimeState(
    val paused: Boolean = true,
    val playbackActive: Boolean = false,
) {
    fun withBooleanProperty(property: String, value: Boolean): MpvRuntimeState = when (property) {
        "pause" -> copy(paused = value)
        "playback-active" -> copy(playbackActive = value)
        else -> this
    }
}

internal fun PlaybackState.reduceMpvRuntime(runtime: MpvRuntimeState): PlaybackState {
    if (phase !in setOf(PlaybackPhase.Ready, PlaybackPhase.Buffering)) return this
    val reducedPhase = when {
        runtime.playbackActive -> PlaybackPhase.Ready
        !runtime.paused -> PlaybackPhase.Buffering
        else -> phase
    }
    return copy(
        phase = reducedPhase,
        isPlaying = !runtime.paused && runtime.playbackActive,
        playWhenReady = !runtime.paused,
    )
}

internal fun PlaybackState.reduceMpvEnded(): PlaybackState = copy(
    phase = PlaybackPhase.Ended,
    isPlaying = false,
    playWhenReady = false,
    positionMs = durationMs.takeIf { it > 0L } ?: positionMs,
)

internal fun PlaybackState.reduceMpvError(message: String): PlaybackState = copy(
    phase = PlaybackPhase.Error,
    isPlaying = false,
    playWhenReady = false,
    errorMessage = message,
)

/**
 * Identifies the currently pending load. A timeout for an older quality can therefore never fail
 * the replacement quality that superseded it.
 */
internal class MpvLoadSession {
    private var nextToken = 0L
    private val issuedTickets = ArrayDeque<MpvLoadTicket>()
    private var nativeTicket: MpvLoadTicket? = null
    private var pendingTicket: MpvLoadTicket? = null
    private var loadedTicket: MpvLoadTicket? = null

    val isPending: Boolean
        get() = synchronized(this) { pendingTicket != null }

    @Synchronized
    fun begin(expectedPath: String): MpvLoadTicket {
        nextToken = if (nextToken == Long.MAX_VALUE) 1L else nextToken + 1L
        return MpvLoadTicket(nextToken, expectedPath).also { ticket ->
            issuedTickets.addLast(ticket)
            pendingTicket = ticket
        }
    }

    /** Associates MPV's ordered START_FILE stream with the load command that produced it. */
    @Synchronized
    fun onNativeStart(actualPath: String?): MpvNativeLoadEvent? {
        val pathMatch = actualPath?.let { path ->
            issuedTickets.firstOrNull { it.expectedPath == path }
        }
        nativeTicket = pathMatch ?: issuedTickets.firstOrNull()
        nativeTicket?.let { issuedTickets.remove(it) }
        return nativeTicket?.toEvent(actualPath)
    }

    /** Captures generation and path on the native callback thread before main-thread dispatch. */
    @Synchronized
    fun snapshotNativeEvent(actualPath: String?): MpvNativeLoadEvent? =
        nativeTicket?.toEvent(actualPath)

    /** END_FILE finishes the native ticket but not necessarily the latest requested ticket. */
    @Synchronized
    fun onNativeEnd(actualPath: String?): MpvNativeLoadEvent? =
        nativeTicket?.toEvent(actualPath).also { nativeTicket = null }

    @Synchronized
    fun isPendingEvent(event: MpvNativeLoadEvent): Boolean =
        pendingTicket.matchesIdentity(event)

    @Synchronized
    fun complete(event: MpvNativeLoadEvent): Boolean {
        val ticket = pendingTicket ?: return false
        if (!ticket.matchesFileLoaded(event)) return false
        pendingTicket = null
        loadedTicket = ticket
        issuedTickets.clear()
        return true
    }

    @Synchronized
    fun expire(ticket: MpvLoadTicket): Boolean {
        if (pendingTicket != ticket) return false
        pendingTicket = null
        return true
    }

    @Synchronized
    fun isLoadedEvent(event: MpvNativeLoadEvent): Boolean =
        pendingTicket == null && loadedTicket.matchesIdentity(event)

    @Synchronized
    fun consumeLoadedEnd(event: MpvNativeLoadEvent): Boolean {
        if (!loadedTicket.matchesIdentity(event)) return false
        loadedTicket = null
        return true
    }

    @Synchronized
    fun consumePendingEnd(event: MpvNativeLoadEvent): Boolean {
        if (!pendingTicket.matchesIdentity(event)) return false
        pendingTicket = null
        return true
    }

    @Synchronized
    fun cancel() {
        issuedTickets.clear()
        nativeTicket = null
        pendingTicket = null
        loadedTicket = null
    }

    /** Removes a command that failed synchronously before MPV could emit START_FILE. */
    @Synchronized
    fun discardUnsubmitted(ticket: MpvLoadTicket) {
        issuedTickets.remove(ticket)
        if (pendingTicket == ticket) pendingTicket = null
        if (nativeTicket == ticket) nativeTicket = null
    }

    @Synchronized
    fun release() {
        issuedTickets.clear()
        nativeTicket = null
        pendingTicket = null
        loadedTicket = null
    }
}

internal data class MpvLoadTicket(
    val generation: Long,
    val expectedPath: String,
)

internal data class MpvNativeLoadEvent(
    val generation: Long,
    val expectedPath: String,
    val actualPath: String?,
)

private fun MpvLoadTicket.toEvent(actualPath: String?): MpvNativeLoadEvent =
    MpvNativeLoadEvent(generation, expectedPath, actualPath)

private fun MpvLoadTicket?.matchesIdentity(event: MpvNativeLoadEvent): Boolean {
    val ticket = this ?: return false
    return ticket.generation == event.generation && ticket.expectedPath == event.expectedPath
}

private fun MpvLoadTicket.matchesFileLoaded(event: MpvNativeLoadEvent): Boolean =
    matchesIdentity(event) && expectedPath == event.actualPath

/** Thread-safe coalescing gate for publishing native progress no faster than the UI interval. */
internal class MpvProgressPublishGate {
    private var scheduled = false

    @Synchronized
    fun requestSchedule(): Boolean {
        if (scheduled) return false
        scheduled = true
        return true
    }

    @Synchronized
    fun consumeScheduled(): Boolean {
        if (!scheduled) return false
        scheduled = false
        return true
    }

    @Synchronized
    fun cancel(): Boolean {
        if (!scheduled) return false
        scheduled = false
        return true
    }
}

/** Owns detached local-media file descriptors and closes every descriptor at most once. */
internal class MpvFileDescriptorOwner(
    private val closeDescriptor: (Int) -> Unit,
) {
    private var current: Int? = null
    private val stale = linkedSetOf<Int>()

    fun replaceWith(fileDescriptor: Int?) {
        current?.let(stale::add)
        current = fileDescriptor
    }

    fun onFileLoaded() {
        closeStale()
    }

    fun onPlaybackEnded() {
        closeAll()
    }

    fun release() {
        closeAll()
    }

    private fun closeStale() {
        val descriptors = stale.toList()
        stale.clear()
        descriptors.forEach(closeDescriptor)
    }

    private fun closeAll() {
        closeStale()
        val descriptor = current
        current = null
        descriptor?.let(closeDescriptor)
    }
}
