package com.yenaly.han1meviewer.ui.screen.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Brightness7
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.playback.compose.VideoKeyframeUiState
import com.yenaly.han1meviewer.playback.compose.VideoPlayerActions
import com.yenaly.han1meviewer.playback.compose.VideoPlayerUiState
import com.yenaly.han1meviewer.playback.compose.formatKeyframeCountdown
import com.yenaly.han1meviewer.playback.compose.positionFromFraction
import com.yenaly.han1meviewer.playback.model.PlaybackCapabilities
import com.yenaly.han1meviewer.playback.model.PlaybackDefaults
import com.yenaly.han1meviewer.playback.model.PlaybackPhase
import com.yenaly.han1meviewer.playback.model.PlaybackSource
import com.yenaly.han1meviewer.playback.model.PlaybackState
import com.yenaly.han1meviewer.playback.model.QualityVariant
import com.yenaly.han1meviewer.playback.model.formatPlaybackTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlin.math.abs
import kotlin.math.max

private const val ControlsAutoHideMillis = 3_000L
private const val VerticalGestureScale = 3f

/**
 * Engine-agnostic player chrome. The actual video output is supplied through [surface].
 */
@Composable
fun VideoPlayerUi(
    state: VideoPlayerUiState,
    actions: VideoPlayerActions,
    surface: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    var gestureFeedback by remember { mutableStateOf<GestureFeedback?>(null) }

    LaunchedEffect(
        state.controlsVisible,
        state.playback.isPlaying,
        state.playback.phase,
    ) {
        if (state.controlsVisible && state.playback.isPlaying && state.playback.phase == PlaybackPhase.Ready) {
            delay(ControlsAutoHideMillis)
            actions.onControlsVisibilityChanged(false)
        }
    }

    Box(
        modifier = modifier.background(Color.Black),
    ) {
        surface()

        if (state.showPoster && !state.posterUri.isNullOrBlank()) {
            AsyncImage(
                model = state.posterUri,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }

        PlayerGestureLayer(
            state = state,
            actions = actions,
            onFeedbackChanged = { gestureFeedback = it },
            modifier = Modifier.matchParentSize(),
        )

        AnimatedVisibility(
            visible = state.controlsVisible && !state.isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            PlayerChromeGradients()
        }

        AnimatedVisibility(
            visible = state.controlsVisible && !state.isLocked,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            PlayerTopControls(state = state, actions = actions)
        }

        PlayerCenterState(
            state = state,
            actions = actions,
            modifier = Modifier.align(Alignment.Center),
        )

        state.keyframeCountdown?.let { countdown ->
            KeyframeCountdownOverlay(
                remainingMs = countdown.remainingMs,
                prompt = countdown.prompt,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(top = 72.dp, start = 16.dp),
            )
        }

        AnimatedVisibility(
            visible = state.controlsVisible,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
        ) {
            FilledIconButton(
                onClick = { actions.onLockChanged(!state.isLocked) },
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(44.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.52f),
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = if (state.isLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                    contentDescription = stringResource(
                        if (state.isLocked) R.string.unlock_method else R.string.use_lock_screen,
                    ),
                )
            }
        }

        AnimatedVisibility(
            visible = state.controlsVisible && !state.isLocked,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            PlayerBottomControls(state = state, actions = actions)
        }

        AnimatedVisibility(
            visible = state.showBottomProgress &&
                !state.controlsVisible &&
                !state.isError &&
                state.playback.durationMs > 0L,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LinearProgressIndicator(
                progress = { state.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                trackColor = Color.White.copy(alpha = 0.18f),
            )
        }

        AnimatedVisibility(
            visible = state.showRestartFromBeginning && !state.isLocked,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 88.dp),
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
        ) {
            ElevatedButton(onClick = actions::onRestartFromBeginning) {
                Text(stringResource(R.string.start_from_beginning))
            }
        }

        gestureFeedback?.let { feedback ->
            GestureIndicatorOverlay(
                visible = true,
                type = feedback.type,
                percent = feedback.fraction,
                text = feedback.text,
                modifier = Modifier.matchParentSize(),
            )
        }
    }

    if (state.keyframePanelVisible) {
        KeyframePanel(
            keyframes = state.keyframes,
            onSelect = actions::onSelectKeyframe,
            onDismiss = actions::onDismissKeyframes,
        )
    }
}

@Composable
private fun PlayerChromeGradients() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(132.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.86f)),
                    ),
                ),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerTopControls(
    state: VideoPlayerUiState,
    actions: VideoPlayerActions,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = actions::onBack) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Color.White,
            )
        }
        IconButton(onClick = actions::onHome) {
            Icon(
                Icons.Outlined.Home,
                contentDescription = stringResource(R.string.home_page),
                tint = Color.White,
            )
        }
        Text(
            text = state.title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall,
        )

        if (state.keyframesEnabled) {
            CompactChip(
                text = stringResource(R.string.h_keyframe),
                modifier = Modifier.combinedClickable(
                    onClickLabel = stringResource(R.string.h_keyframe),
                    role = Role.Button,
                    onLongClickLabel = stringResource(R.string.add_to_h_keyframe),
                    onClick = actions::onOpenKeyframes,
                    onLongClick = actions::onAddKeyframe,
                ),
            )
            Spacer(Modifier.width(6.dp))
        }

        if (state.capabilities.supportsPlaybackSpeed) {
            SpeedMenu(state = state, onSpeedSelected = actions::onSetSpeed)
            Spacer(Modifier.width(6.dp))
        }

        if (state.capabilities.supportsSuperResolution) {
            SuperResolutionMenu(
                selectedIndex = state.superResolutionIndex,
                onSelected = actions::onSetSuperResolution,
            )
        }
    }
}

@Composable
private fun PlayerCenterState(
    state: VideoPlayerUiState,
    actions: VideoPlayerActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            state.isError -> ErrorCard(
                message = state.playback.errorMessage,
                onRetry = actions::onRetry,
            )

            state.isEnded -> FilledTonalButton(onClick = actions::onReplay) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.start_from_beginning))
            }

            state.isLoading -> CircularProgressIndicator(color = Color.White)

            state.controlsVisible && !state.isLocked -> FilledIconButton(
                onClick = actions::onTogglePlayPause,
                modifier = Modifier.size(68.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                ),
            ) {
                Icon(
                    imageVector = if (state.playback.isPlaying) {
                        Icons.Outlined.Pause
                    } else {
                        Icons.Outlined.PlayArrow
                    },
                    contentDescription = stringResource(R.string.play_pause),
                    modifier = Modifier.size(40.dp),
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String?, onRetry: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = message?.takeIf(String::isNotBlank)
                    ?: stringResource(R.string.load_failed_retry),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(14.dp))
            FilledTonalButton(onClick = onRetry) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun PlayerBottomControls(
    state: VideoPlayerUiState,
    actions: VideoPlayerActions,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.Black.copy(alpha = 0.42f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.08f),
        ),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            PlayerSlider(
                positionMs = state.playback.positionMs,
                durationMs = state.playback.durationMs,
                bufferedFraction = state.bufferedFraction,
                enabled = state.canSeek,
                onSeek = actions::onSeekTo,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = actions::onTogglePlayPause,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = if (state.playback.isPlaying) {
                            Icons.Outlined.Pause
                        } else {
                            Icons.Outlined.PlayArrow
                        },
                        contentDescription = stringResource(R.string.play_pause),
                        tint = Color.White,
                    )
                }
                Text(
                    text = "${formatPlaybackTime(state.playback.positionMs)} / " +
                        formatPlaybackTime(state.playback.durationMs),
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.weight(1f))
                if (state.capabilities.supportsQualitySelection && state.qualities.size > 1) {
                    QualityMenu(
                        qualities = state.qualities,
                        selectedQualityId = state.playback.selectedQualityId,
                        onQualitySelected = actions::onSelectQuality,
                    )
                } else {
                    state.selectedQuality?.let {
                        Text(
                            text = it.label,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                IconButton(
                    onClick = actions::onToggleFullscreen,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (state.isFullscreen) {
                            Icons.Outlined.FullscreenExit
                        } else {
                            Icons.Outlined.Fullscreen
                        },
                        contentDescription = stringResource(
                            if (state.isFullscreen) {
                                R.string.report_portrait
                            } else {
                                R.string.report_landscape
                            },
                        ),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerSlider(
    positionMs: Long,
    durationMs: Long,
    bufferedFraction: Float,
    enabled: Boolean,
    onSeek: (Long) -> Unit,
) {
    var scrubbingPositionMs by remember { mutableStateOf<Long?>(null) }
    val displayedPositionMs = scrubbingPositionMs ?: positionMs
    val value = if (durationMs > 0L) {
        displayedPositionMs.toFloat() / durationMs.toFloat()
    } else {
        0f
    }

    Slider(
        value = value.coerceIn(0f, 1f),
        onValueChange = { scrubbingPositionMs = positionFromFraction(it, durationMs) },
        onValueChangeFinished = {
            scrubbingPositionMs?.let(onSeek)
            scrubbingPositionMs = null
        },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
        thumb = {
            Box(
                Modifier
                    .size(12.dp)
                    .background(Color.White, CircleShape),
            )
        },
        track = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f)),
                )
                Box(
                    Modifier
                        .fillMaxWidth(bufferedFraction.coerceIn(0f, 1f))
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.34f)),
                )
                Box(
                    Modifier
                        .fillMaxWidth(value.coerceIn(0f, 1f))
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        },
    )
}

@Composable
private fun QualityMenu(
    qualities: List<QualityVariant>,
    selectedQualityId: String?,
    onQualitySelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = qualities.firstOrNull { it.id == selectedQualityId } ?: qualities.first()
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selected.label, color = Color.White)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            qualities.forEach { quality ->
                DropdownMenuItem(
                    text = { Text(quality.label) },
                    onClick = {
                        expanded = false
                        onQualitySelected(quality.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun SpeedMenu(
    state: VideoPlayerUiState,
    onSpeedSelected: (Float) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        CompactChip(
            text = "${state.playback.speed}x",
            modifier = Modifier.clickable { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.availableSpeeds.forEach { speed ->
                DropdownMenuItem(
                    text = { Text("${speed}x") },
                    onClick = {
                        expanded = false
                        onSpeedSelected(speed)
                    },
                )
            }
        }
    }
}

@Composable
private fun SuperResolutionMenu(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val labels = listOf(
        stringResource(R.string.super_resolution_off),
        stringResource(R.string.super_resolution_performance),
        stringResource(R.string.super_resolution_quality),
    )
    Box {
        CompactChip(
            text = stringResource(R.string.anime_4k).replace('\n', ' '),
            modifier = Modifier.clickable { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            labels.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = {
                        Text(
                            if (index == selectedIndex) "✓ $label" else label,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(index)
                    },
                )
            }
        }
    }
}

@Composable
private fun CompactChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun KeyframeCountdownOverlay(
    remainingMs: Long,
    prompt: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.Black.copy(alpha = 0.58f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.1f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.h_keyframe),
                color = Color.White.copy(alpha = 0.78f),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = formatKeyframeCountdown(remainingMs),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
            )
            prompt?.takeIf(String::isNotBlank)?.let {
                Text(
                    text = it,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun KeyframePanel(
    keyframes: List<VideoKeyframeUiState>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.h_keyframe)) },
        text = {
            if (keyframes.isEmpty()) {
                Text(stringResource(R.string.here_is_empty))
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(keyframes, key = { it.positionMs }) { keyframe ->
                        ListItem(
                            supportingContent = {
                                keyframe.prompt?.takeIf(String::isNotBlank)?.let { Text(it) }
                            },
                            modifier = Modifier.clickable { onSelect(keyframe.positionMs) },
                        ) {
                            Text(formatPlaybackTime(keyframe.positionMs))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

private enum class DragMode {
    Seek,
    Brightness,
    Volume,
}

private data class GestureFeedback(
    val type: GestureIndicatorType,
    val fraction: Float,
    val text: String,
)

@Composable
private fun PlayerGestureLayer(
    state: VideoPlayerUiState,
    actions: VideoPlayerActions,
    onFeedbackChanged: (GestureFeedback?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val latestState by rememberUpdatedState(state)
    val latestActions by rememberUpdatedState(actions)
    val latestFeedbackCallback by rememberUpdatedState(onFeedbackChanged)
    val latestHapticFeedback by rememberUpdatedState(hapticFeedback)

    val tapModifier = Modifier.pointerInput(Unit) {
        detectTapGestures(
            onTap = {
                latestActions.onControlsVisibilityChanged(!latestState.controlsVisible)
            },
            onDoubleTap = {
                if (!latestState.isLocked) latestActions.onTogglePlayPause()
            },
            onPress = {
                val pressState = latestState
                if (pressState.isLocked || !pressState.playback.isPlaying) {
                    return@detectTapGestures
                }
                supervisorScope {
                    var speedActivated = false
                    val originalSpeed = pressState.playback.speed
                    val temporarySpeed =
                        (originalSpeed * pressState.longPressSpeedMultiplier).coerceAtLeast(0.1f)
                    val activation = launch {
                        delay(viewConfiguration.longPressTimeoutMillis)
                        speedActivated = true
                        latestHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        latestActions.onSetSpeed(temporarySpeed)
                        latestFeedbackCallback(
                            GestureFeedback(
                                type = GestureIndicatorType.Speed,
                                fraction = (temporarySpeed / max(temporarySpeed, 3f))
                                    .coerceIn(0f, 1f),
                                text = "${temporarySpeed}x",
                            ),
                        )
                    }
                    try {
                        tryAwaitRelease()
                    } finally {
                        activation.cancel()
                        if (speedActivated) {
                            latestActions.onSetSpeed(originalSpeed)
                            latestFeedbackCallback(null)
                        }
                    }
                }
            },
        )
    }

    val dragModifier = Modifier.pointerInput(Unit) {
        var dragMode: DragMode? = null
        var dragStart = Offset.Zero
        var totalDrag = Offset.Zero
        var dragEnabled = false
        var canSeek = false
        var startPositionMs = 0L
        var durationMs = 0L
        var seekSensitivity = 1
        var startBrightness = 0f
        var startVolume = 0f
        var targetPositionMs = 0L

        detectDragGestures(
            onDragStart = { offset ->
                val dragState = latestState
                dragStart = offset
                totalDrag = Offset.Zero
                dragMode = null
                dragEnabled = !dragState.isLocked
                canSeek = dragState.canSeek
                startPositionMs = dragState.playback.positionMs
                durationMs = dragState.playback.durationMs
                seekSensitivity = dragState.seekGestureSensitivity.coerceAtLeast(1)
                startBrightness = dragState.brightness.coerceIn(0f, 1f)
                startVolume = dragState.volume.coerceIn(0f, 1f)
                targetPositionMs = startPositionMs
            },
            onDragCancel = {
                dragMode = null
                latestFeedbackCallback(null)
            },
            onDragEnd = {
                if (dragMode == DragMode.Seek) latestActions.onSeekTo(targetPositionMs)
                dragMode = null
                latestFeedbackCallback(null)
            },
            onDrag = { change, dragAmount ->
                if (!dragEnabled) return@detectDragGestures
                totalDrag += dragAmount
                if (dragMode == null) {
                    dragMode = if (abs(totalDrag.x) >= abs(totalDrag.y)) {
                        if (canSeek) DragMode.Seek else null
                    } else if (dragStart.x < size.width / 2f) {
                        DragMode.Brightness
                    } else {
                        DragMode.Volume
                    }
                }
                val mode = dragMode ?: return@detectDragGestures
                change.consume()
                when (mode) {
                    DragMode.Seek -> {
                        val fractionDelta = totalDrag.x /
                            (size.width.coerceAtLeast(1) * seekSensitivity)
                        targetPositionMs = (startPositionMs + fractionDelta * durationMs)
                            .toLong()
                            .coerceIn(0L, durationMs)
                        latestFeedbackCallback(
                            GestureFeedback(
                                type = GestureIndicatorType.Progress,
                                fraction = if (durationMs > 0L) {
                                    targetPositionMs.toFloat() / durationMs
                                } else {
                                    0f
                                },
                                text = "${formatPlaybackTime(targetPositionMs)} / " +
                                    formatPlaybackTime(durationMs),
                            ),
                        )
                    }

                    DragMode.Brightness -> {
                        val targetBrightness = (
                            startBrightness -
                                totalDrag.y / size.height.coerceAtLeast(1) * VerticalGestureScale
                        ).coerceIn(0f, 1f)
                        latestActions.onBrightnessChanged(targetBrightness)
                        latestFeedbackCallback(
                            GestureFeedback(
                                GestureIndicatorType.Brightness,
                                targetBrightness,
                                "${(targetBrightness * 100).toInt()}%",
                            ),
                        )
                    }

                    DragMode.Volume -> {
                        val targetVolume = (
                            startVolume -
                                totalDrag.y / size.height.coerceAtLeast(1) * VerticalGestureScale
                        ).coerceIn(0f, 1f)
                        latestActions.onVolumeChanged(targetVolume)
                        latestFeedbackCallback(
                            GestureFeedback(
                                GestureIndicatorType.Volume,
                                targetVolume,
                                "${(targetVolume * 100).toInt()}%",
                            ),
                        )
                    }
                }
            },
        )
    }

    Box(modifier.then(tapModifier).then(dragModifier))
}

enum class GestureIndicatorType {
    Brightness,
    Volume,
    Progress,
    Speed,
}

@Composable
fun GestureIndicatorOverlay(
    visible: Boolean,
    type: GestureIndicatorType,
    percent: Float,
    text: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(width = 170.dp, height = 174.dp),
                shape = RoundedCornerShape(32.dp),
                color = Color.Black.copy(alpha = 0.72f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.12f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = when (type) {
                            GestureIndicatorType.Brightness -> Icons.Outlined.Brightness7
                            GestureIndicatorType.Volume -> Icons.AutoMirrored.Outlined.VolumeUp
                            GestureIndicatorType.Progress -> Icons.Outlined.FastForward
                            GestureIndicatorType.Speed -> Icons.Outlined.Speed
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(18.dp))
                    LinearProgressIndicator(
                        progress = { percent.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape),
                        trackColor = Color.White.copy(alpha = 0.14f),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = text,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

private object PreviewVideoPlayerActions : VideoPlayerActions {
    override fun onBack() = Unit
    override fun onHome() = Unit
    override fun onTogglePlayPause() = Unit
    override fun onRetry() = Unit
    override fun onReplay() = Unit
    override fun onSeekTo(positionMs: Long) = Unit
    override fun onSelectQuality(qualityId: String) = Unit
    override fun onSetSpeed(speed: Float) = Unit
    override fun onSetSuperResolution(index: Int) = Unit
    override fun onOpenKeyframes() = Unit
    override fun onDismissKeyframes() = Unit
    override fun onSelectKeyframe(positionMs: Long) = Unit
    override fun onAddKeyframe() = Unit
    override fun onToggleFullscreen() = Unit
    override fun onControlsVisibilityChanged(visible: Boolean) = Unit
    override fun onLockChanged(locked: Boolean) = Unit
    override fun onBrightnessChanged(fraction: Float) = Unit
    override fun onVolumeChanged(fraction: Float) = Unit
    override fun onRestartFromBeginning() = Unit
}

private val PreviewSource = PlaybackSource(
    id = "preview",
    title = "Preview video",
    qualities = listOf(
        QualityVariant("720p", "720P", "https://example.invalid/720.m3u8"),
        QualityVariant("1080p", "1080P", "https://example.invalid/1080.m3u8"),
    ),
    preferredQualityId = "1080p",
)

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 960, heightDp = 540)
@Composable
private fun VideoPlayerUiPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        VideoPlayerUi(
            state = VideoPlayerUiState(
                playback = PlaybackState(
                    source = PreviewSource,
                    phase = PlaybackPhase.Ready,
                    positionMs = 756_000L,
                    durationMs = 1_452_000L,
                    bufferedPositionMs = 1_050_000L,
                    speed = 1.5f,
                    selectedQualityId = "1080p",
                ),
                capabilities = PlaybackCapabilities(supportsSuperResolution = true),
                longPressSpeedMultiplier = PlaybackDefaults.DEFAULT_LONG_PRESS_SPEED_MULTIPLIER,
                seekGestureSensitivity = PlaybackDefaults.DEFAULT_PROGRESS_SLIDE_SENSITIVITY,
                keyframesEnabled = true,
                keyframes = listOf(VideoKeyframeUiState(600_000L, "Preview keyframe")),
            ),
            actions = PreviewVideoPlayerActions,
            surface = { Box(Modifier.matchParentSize().background(Color.Black)) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 960, heightDp = 540)
@Composable
private fun VideoPlayerUiLoadingPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        VideoPlayerUi(
            state = VideoPlayerUiState(
                playback = PlaybackState(source = PreviewSource, phase = PlaybackPhase.Buffering),
                capabilities = PlaybackCapabilities(),
                longPressSpeedMultiplier = PlaybackDefaults.DEFAULT_LONG_PRESS_SPEED_MULTIPLIER,
                seekGestureSensitivity = PlaybackDefaults.DEFAULT_PROGRESS_SLIDE_SENSITIVITY,
            ),
            actions = PreviewVideoPlayerActions,
            surface = { Box(Modifier.matchParentSize().background(Color.Black)) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
