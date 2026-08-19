package io.github.darriousliu.han1meviewer.feature.video.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import io.github.darriousliu.han1meviewer.core.common.PlayerDefaults
import io.github.darriousliu.han1meviewer.core.common.util.formatVideoTime
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import kotlin.math.abs
import kotlin.math.roundToInt

/** 手势指示浮层要展示的内容，由手势层写、总装层读。 */
@Stable
internal class PlayerGestureState {
    var indicatorVisible by mutableStateOf(false)
    var indicatorType by mutableStateOf(GestureIndicatorType.Progress)
    var indicatorPercent by mutableFloatStateOf(0f)
    var indicatorText by mutableStateOf("")

    /** 长按倍速激活中；期间忽略一切位移（HJzvdStd :777 的互斥）。 */
    var boosting by mutableStateOf(false)
}

@Composable
internal fun rememberPlayerGestureState(): PlayerGestureState = remember { PlayerGestureState() }

private enum class DragMode { None, Progress, Brightness, Volume }

/**
 * 手势层，铺满播放器区域。语义照 `HJzvdStd`：
 *
 * - 单击：切换控件显隐（锁定态只切锁图标）
 * - 双击：播放/暂停（仅播放/暂停态）
 * - 长按（播放中、未在滑动）：按 `longPressSpeedTime` 倍速，抬手还原，带触觉反馈
 * - 横滑：调进度（含 `slideSensitivity` 换算；拖动只预览、抬手 seek；出错/未就绪不响应）
 * - 左半屏竖滑亮度 / 右半屏竖滑音量（实时生效；分母用窗口高，与旧屏幕高几乎一致）
 * - **非全屏也全部生效**；锁定时只响应单击
 */
@Composable
internal fun PlayerGestureLayer(
    controller: VideoPlayerController,
    uiState: PlayerUiState,
    gestureState: PlayerGestureState,
    deviceControls: DeviceMediaControls,
    onDoubleTapPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val windowInfo = LocalWindowInfo.current

    Box(
        modifier = modifier
            .fillMaxSize()
            // 层 1：点按与长按倍速
            .pointerInput(controller, uiState, gestureState) {
                detectTapGestures(
                    onTap = { uiState.toggleControls() },
                    onDoubleTap = {
                        if (uiState.locked) return@detectTapGestures
                        val visual = playbackVisualOf(controller)
                        if (visual == PlaybackVisual.Playing || visual == PlaybackVisual.Paused) {
                            onDoubleTapPlayPause()
                        }
                    },
                    onLongPress = {
                        // gestureActive 时不触发：位移小于系统 touch slop 但已进滑动模式的场景
                        if (!uiState.locked && !uiState.gestureActive && controller.isPlaying) {
                            gestureState.boosting = true
                            uiState.gestureActive = true
                            controller.boostSpeed(Preferences.longPressSpeedTime)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onPress = {
                        tryAwaitRelease()
                        if (gestureState.boosting) {
                            controller.restoreSpeed()
                            gestureState.boosting = false
                            uiState.gestureActive = false
                        }
                    },
                )
            }
            // 层 2：三向滑动
            .pointerInput(controller, uiState, gestureState, deviceControls) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (uiState.locked) return@awaitEachGesture
                    var mode = DragMode.None
                    var downPositionMs = 0L
                    var pendingSeekMs = 0L
                    var downBrightness = 0f
                    var downVolume = 0f
                    val windowSize = windowInfo.containerSize
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (mode == DragMode.Progress) controller.seekTo(pendingSeekMs)
                            break
                        }
                        if (gestureState.boosting) continue
                        val total = change.position - down.position
                        if (mode == DragMode.None) {
                            mode = when {
                                abs(total.x) >= GESTURE_THRESHOLD_PX -> {
                                    // 出错態不响应进度手势（HJzvdStd :790）；未就绪（时长 0）同理
                                    if (controller.error == null && controller.durationMs > 0L) {
                                        downPositionMs = controller.positionMs
                                        DragMode.Progress
                                    } else {
                                        DragMode.None
                                    }
                                }

                                abs(total.y) >= GESTURE_THRESHOLD_PX -> {
                                    if (down.position.x < size.width / 2f) {
                                        downBrightness = deviceControls.brightness
                                        DragMode.Brightness
                                    } else {
                                        downVolume = deviceControls.volumePercent
                                        DragMode.Volume
                                    }
                                }

                                else -> DragMode.None
                            }
                            if (mode != DragMode.None) {
                                uiState.gestureActive = true
                                gestureState.indicatorVisible = true
                            }
                        }
                        if (mode == DragMode.None) continue
                        change.consume()
                        when (mode) {
                            DragMode.Progress -> {
                                val duration = controller.durationMs
                                val sensitivity =
                                    PlayerDefaults.toRealSensitivity(Preferences.slideSensitivity)
                                pendingSeekMs = (downPositionMs +
                                        total.x * duration / (windowSize.width * sensitivity))
                                    .toLong()
                                    .coerceIn(0L, duration)
                                gestureState.indicatorType = GestureIndicatorType.Progress
                                gestureState.indicatorPercent =
                                    if (duration > 0L) pendingSeekMs.toFloat() / duration else 0f
                                gestureState.indicatorText =
                                    "${formatVideoTime(pendingSeekMs)} / ${formatVideoTime(duration)}"
                            }

                            DragMode.Brightness -> {
                                // deltaY*3/高：HJzvdStd :856-873；下限 0.01f 由 actual 保证
                                val value = (downBrightness + (-total.y) * 3f / windowSize.height)
                                    .coerceIn(DeviceMediaControls.MIN_BRIGHTNESS, 1f)
                                deviceControls.brightness = value
                                gestureState.indicatorType = GestureIndicatorType.Brightness
                                gestureState.indicatorPercent = value
                                gestureState.indicatorText = "${(value * 100).roundToInt()}%"
                            }

                            DragMode.Volume -> {
                                val value = (downVolume + (-total.y) * 3f / windowSize.height)
                                    .coerceIn(0f, 1f)
                                deviceControls.volumePercent = value
                                gestureState.indicatorType = GestureIndicatorType.Volume
                                gestureState.indicatorPercent = value
                                gestureState.indicatorText = "${(value * 100).roundToInt()}%"
                            }

                            DragMode.None -> Unit
                        }
                    }
                    if (mode != DragMode.None) {
                        uiState.gestureActive = false
                        gestureState.indicatorVisible = false
                    }
                }
            }
    )
}

/** jzvd `THRESHOLD`：10 **像素**（原值就是 px，不换 dp，保真）。 */
private const val GESTURE_THRESHOLD_PX = 10f
