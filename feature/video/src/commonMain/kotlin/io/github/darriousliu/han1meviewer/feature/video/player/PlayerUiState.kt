package io.github.darriousliu.han1meviewer.feature.video.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.darriousliu.han1meviewer.core.common.PlayerDefaults
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import kotlinx.coroutines.delay

/** 右侧滑入面板当前展示的内容。 */
enum class PlayerMenu { None, Speed, Clarity, HKeyframes, SuperResolution }

/** 播放器画面此刻的「视觉状态」，控件显隐矩阵按它分派。 */
enum class PlaybackVisual { Preparing, Buffering, Playing, Paused, Ended, Error }

/** 各控件此刻是否渲染。由 [controlsFor] 从状态推出，本身无状态。 */
data class ControlsVisibility(
    val topBar: Boolean,
    val bottomBar: Boolean,
    val centerButton: Boolean,
    val loading: Boolean,
    val retry: Boolean,
    val replayText: Boolean,
    val lockButton: Boolean,
    val miniProgress: Boolean,
)

/**
 * 控件层自己的 UI 状态（显隐/锁定/菜单/拖动预览/倍速档位）。
 * 播放事实在 [VideoPlayerController]，页面级状态在宿主——这里只管控件。
 */
@Stable
class PlayerUiState internal constructor(initialSpeedIndex: Int) {

    var controlsVisible by mutableStateOf(true)

    /** 全屏锁定：手势与控件全部失效，只留锁图标本身可点。 */
    var locked by mutableStateOf(false)

    var activeMenu by mutableStateOf(PlayerMenu.None)

    var currentSpeedIndex by mutableIntStateOf(initialSpeedIndex)

    /** 进度条拖动中（只预览不 seek）。 */
    var scrubbing by mutableStateOf(false)
    var scrubFraction by mutableFloatStateOf(0f)

    /** 亮度/音量/滑动进度手势进行中；期间不响应单击显隐、不自动隐藏。 */
    var gestureActive by mutableStateOf(false)

    fun toggleControls() {
        controlsVisible = !controlsVisible
    }

    /** 打开菜单前先把控件层收掉（对应 jzvd 的 onCLickUiToggleToClear）。 */
    fun openMenu(menu: PlayerMenu) {
        controlsVisible = false
        activeMenu = menu
    }

    fun dismissMenu() {
        activeMenu = PlayerMenu.None
    }
}

/**
 * @param controller 用于自动隐藏计时（播放中才计时）与默认倍速应用。
 */
@Composable
fun rememberPlayerUiState(controller: VideoPlayerController): PlayerUiState {
    val state = remember(controller) {
        val savedSpeed = Preferences.playerSpeed
        val index = PlayerDefaults.SPEED_ARRAY.indexOfFirst { it == savedSpeed }
            .takeIf { it >= 0 } ?: PlayerDefaults.DEF_SPEED_INDEX
        PlayerUiState(initialSpeedIndex = index)
    }

    // 默认倍速：ExoPlayer 的 playbackParameters 跨 setMediaSource 保留，构造期设一次即可
    LaunchedEffect(controller) {
        val speed = PlayerDefaults.SPEED_ARRAY.getOrElse(state.currentSpeedIndex) { 1f }
        if (speed != 1f) controller.setSpeed(speed)
    }

    // 自动隐藏：仅播放中、无菜单、非拖动、非手势中计时；暂停态不隐藏（jzvd 同款）
    LaunchedEffect(
        state.controlsVisible,
        controller.isPlaying,
        state.activeMenu,
        state.scrubbing,
        state.gestureActive,
    ) {
        if (state.controlsVisible && controller.isPlaying &&
            state.activeMenu == PlayerMenu.None && !state.scrubbing && !state.gestureActive
        ) {
            delay(AUTO_HIDE_CONTROLS_MS)
            state.controlsVisible = false
        }
    }
    return state
}

/** jzvd `DismissControlViewTimerTask` 的真值是 2500ms（不是 3 秒）。 */
internal const val AUTO_HIDE_CONTROLS_MS = 2500L

/** 从播放事实推出视觉状态。 */
fun playbackVisualOf(controller: VideoPlayerController): PlaybackVisual = when {
    controller.error != null -> PlaybackVisual.Error
    controller.isEnded -> PlaybackVisual.Ended
    controller.isBuffering && !controller.firstFrameRendered -> PlaybackVisual.Preparing
    controller.isBuffering -> PlaybackVisual.Buffering
    controller.isPlaying -> PlaybackVisual.Playing
    else -> PlaybackVisual.Paused
}

/**
 * 显隐矩阵（照 jzvd `changeUiToXxx` 系列 + `HJzvdStd` 覆写）：
 *
 * | 状态 | visible=true | visible=false |
 * |---|---|---|
 * | Preparing | 顶栏+loading（无底栏无播放钮，#issue-232） | loading |
 * | Buffering | 顶栏+底栏+loading | loading |
 * | Playing | 顶栏+底栏+暂停钮+锁(全屏) | 迷你进度条 |
 * | Paused | 顶栏+底栏+播放钮+锁(全屏) | 迷你进度条 |
 * | Ended | 顶栏+replay+重播文字（无底栏，迷你条满格） | 同左 |
 * | Error | 顶栏+重试布局 | 同左 |
 * | 锁定(全屏) | 只显示锁 | 无 |
 */
fun controlsFor(
    visual: PlaybackVisual,
    controlsVisible: Boolean,
    locked: Boolean,
    isFullscreen: Boolean,
    showBottomProgressPref: Boolean,
): ControlsVisibility {
    if (locked) {
        return ControlsVisibility(
            topBar = false, bottomBar = false, centerButton = false,
            loading = visual == PlaybackVisual.Preparing || visual == PlaybackVisual.Buffering,
            retry = false, replayText = false,
            lockButton = controlsVisible,
            miniProgress = false,
        )
    }
    val playingOrPaused = visual == PlaybackVisual.Playing || visual == PlaybackVisual.Paused
    return ControlsVisibility(
        topBar = when (visual) {
            PlaybackVisual.Ended, PlaybackVisual.Error -> true
            else -> controlsVisible
        },
        bottomBar = when (visual) {
            PlaybackVisual.Buffering, PlaybackVisual.Playing, PlaybackVisual.Paused -> controlsVisible
            else -> false
        },
        centerButton = playingOrPaused && controlsVisible,
        loading = visual == PlaybackVisual.Preparing || visual == PlaybackVisual.Buffering,
        retry = visual == PlaybackVisual.Error,
        replayText = visual == PlaybackVisual.Ended,
        lockButton = isFullscreen && playingOrPaused && controlsVisible,
        miniProgress = when (visual) {
            PlaybackVisual.Ended -> showBottomProgressPref
            PlaybackVisual.Playing, PlaybackVisual.Paused, PlaybackVisual.Buffering ->
                showBottomProgressPref && !controlsVisible
            else -> false
        },
    )
}
