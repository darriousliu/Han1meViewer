package io.github.darriousliu.han1meviewer.feature.video.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.darriousliu.han1meviewer.core.common.PlayerDefaults
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.anime_4k
import io.github.darriousliu.han1meviewer.core.resource.pause_then_long_press
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.storage.entity.HKeyframeEntity
import io.github.darriousliu.han1meviewer.core.ui.component.LocalToaster
import io.github.darriousliu.han1meviewer.core.ui.component.showShort
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * Media3 播放器的控件层总装。UI 结构与显隐语义复刻线上 jzvd 那套
 * （`layout_jzvd_with_speed.xml` + `HJzvdStd`），显隐矩阵见 [controlsFor]。
 *
 * 平台差异走参数注入的接口（[PlayerScreenController] / [PlayerEnvironment]），
 * 播放事实全在 [VideoPlayerController]——本文件纯 commonMain。
 */
@Composable
fun HanimeVideoPlayer(
    controller: VideoPlayerController,
    title: String,
    posterUrl: String?,
    qualityKeys: List<String>,
    currentQuality: String?,
    onSelectQuality: (String) -> Unit,
    hKeyframes: HKeyframeEntity?,
    savedProgressMs: Long,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
    isInPip: Boolean,
    hasPlayableSource: Boolean,
    onBlockedPlayClick: () -> Unit,
    onRetry: () -> Unit,
    onRequestAddKeyframe: (positionMs: Long) -> Unit,
    modifier: Modifier = Modifier,
    showSuperResolution: Boolean = false,
    superResolutionIndex: Int = 0,
    onSelectSuperResolution: (Int) -> Unit = {},
    screen: PlayerScreenController = NoopPlayerScreenController,
    environment: PlayerEnvironment = NoopPlayerEnvironment,
) {
    val uiState = rememberPlayerUiState(controller)
    val gestureState = rememberPlayerGestureState()
    val deviceControls = rememberDeviceMediaControls()
    val toaster = LocalToaster.current
    val pauseThenLongPressHint = stringResource(Res.string.pause_then_long_press)

    // 100ms 打点驱动进度类控件重组（controller 的 position 不是 State）。
    // 和 HJzvdStd 把 jzvd 300ms 改 100ms 同理：H 帧倒计时要亚秒精度。
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(controller) {
        while (true) {
            delay(PLAYER_TICK_INTERVAL_MS)
            tick++
        }
    }

    val visual = playbackVisualOf(controller)
    val visibility = controlsFor(
        visual = visual,
        controlsVisible = uiState.controlsVisible,
        locked = uiState.locked,
        isFullscreen = isFullscreen,
        showBottomProgressPref = Preferences.showBottomProgress,
    )
    // 控件层展开那一刻取一次电量/时间（jzvd 的 setSystemTimeAndBattery 同款时机）
    val batteryPercent = remember(uiState.controlsVisible, environment) {
        environment.batteryPercent()
    }
    val timeText = remember(uiState.controlsVisible, environment) {
        environment.currentTimeText()
    }

    // 全屏时控件避开状态栏/刘海/导航栏的横向区域（HJzvdStd :628-633 的等价）
    val chromeInsetsModifier = if (isFullscreen) {
        Modifier.windowInsetsPadding(
            WindowInsets.systemBars.union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal)
        )
    } else {
        Modifier
    }

    Box(modifier.background(Color.Black).clipToBounds()) {
        VideoSurfaceFit(controller)

        // 封面：首帧渲染前显示；完播不再回到封面（HJzvdStd.onCompletion 语义）
        if (posterUrl != null && !controller.firstFrameRendered) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (isInPip) {
            // PiP 里只留画面和加载圈（setControlsVisible(false) 的等价）
            if (visibility.loading) {
                PlayerLoadingIndicator(Modifier.align(Alignment.Center))
            }
            return@Box
        }

        PlayerGestureLayer(
            controller = controller,
            uiState = uiState,
            gestureState = gestureState,
            deviceControls = deviceControls,
            onDoubleTapPlayPause = {
                if (controller.isPlaying) controller.pause() else controller.play()
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (visibility.topBar) {
            PlayerTopBar(
                title = title,
                isFullscreen = isFullscreen,
                showHKeyframeEntry = Preferences.hKeyframesEnable,
                speedLabel = uiState.currentSpeedIndex
                    .takeIf { it != PlayerDefaults.DEF_SPEED_INDEX }
                    ?.let { PlayerDefaults.SPEED_LABELS[it] },
                showSuperResolution = showSuperResolution,
                superResolutionLabel = stringResource(Res.string.anime_4k),
                batteryPercent = batteryPercent,
                timeText = timeText,
                onBack = onBack,
                onGoHome = onGoHome,
                onHKeyframeClick = { uiState.openMenu(PlayerMenu.HKeyframes) },
                onHKeyframeLongClick = {
                    if (controller.isPlaying) {
                        toaster.showShort(pauseThenLongPressHint)
                    } else {
                        onRequestAddKeyframe(controller.positionMs)
                    }
                },
                onSpeedClick = { uiState.openMenu(PlayerMenu.Speed) },
                onSuperResolutionClick = { uiState.openMenu(PlayerMenu.SuperResolution) },
                modifier = Modifier.align(Alignment.TopCenter).then(chromeInsetsModifier),
            )
        }

        if (visibility.loading) {
            PlayerLoadingIndicator(Modifier.align(Alignment.Center))
        }

        if (visibility.centerButton || visibility.replayText) {
            PlayerCenterButton(
                visual = visual,
                onPlayPause = {
                    when {
                        !hasPlayableSource -> onBlockedPlayClick()
                        controller.isPlaying -> controller.pause()
                        else -> controller.play()
                    }
                },
                onReplay = {
                    controller.seekTo(0)
                    controller.play()
                },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (visibility.retry) {
            PlayerErrorRetry(
                onRetry = onRetry,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (visibility.lockButton) {
            PlayerLockButton(
                locked = uiState.locked,
                onToggle = { uiState.locked = !uiState.locked },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .then(chromeInsetsModifier)
                    .padding(end = 16.dp),
            )
        }

        if (visibility.bottomBar) {
            PlayerBottomBar(
                controller = controller,
                uiState = uiState,
                tick = tick,
                isFullscreen = isFullscreen,
                currentQuality = currentQuality,
                onQualityClick = { uiState.openMenu(PlayerMenu.Clarity) },
                onToggleFullscreen = onToggleFullscreen,
                modifier = Modifier.align(Alignment.BottomCenter).then(chromeInsetsModifier),
            )
        }

        if (visibility.miniProgress) {
            PlayerMiniProgressBar(
                controller = controller,
                tick = tick,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // 续播钮：进度 >5s、开了记忆播放、首帧后出现，5 秒自动消失；点击回到片头
        var resumeDismissed by remember(controller) { mutableStateOf(false) }
        val showResume = !uiState.locked && !resumeDismissed &&
                savedProgressMs > RESUME_MIN_PROGRESS_MS &&
                Preferences.allowResumePlayback && controller.firstFrameRendered
        if (showResume) {
            LaunchedEffect(Unit) {
                delay(RESUME_AUTO_HIDE_MS)
                resumeDismissed = true
            }
            PlayerResumeButton(
                onClick = {
                    controller.seekTo(0)
                    resumeDismissed = true
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp),
            )
        }

        PlayerSideSheet(
            menu = uiState.activeMenu,
            currentSpeedIndex = uiState.currentSpeedIndex,
            onSelectSpeed = { index ->
                uiState.currentSpeedIndex = index
                controller.setSpeed(PlayerDefaults.SPEED_ARRAY[index])
                uiState.dismissMenu()
            },
            qualityKeys = qualityKeys,
            currentQuality = currentQuality,
            onSelectQuality = { key ->
                uiState.dismissMenu()
                onSelectQuality(key)
            },
            showSuperResolution = showSuperResolution,
            currentSuperResolutionIndex = superResolutionIndex,
            onSelectSuperResolution = { index ->
                uiState.dismissMenu()
                onSelectSuperResolution(index)
            },
            hKeyframes = hKeyframes,
            onSeekToKeyframe = { position -> controller.seekTo(position) },
            onDismiss = { uiState.dismissMenu() },
            modifier = Modifier.fillMaxSize(),
        )

        GestureIndicatorOverlay(
            visible = gestureState.indicatorVisible,
            type = gestureState.indicatorType,
            percent = gestureState.indicatorPercent,
            text = gestureState.indicatorText,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** surface 按视频实际尺寸 fitCenter；未知尺寸时铺满。 */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.VideoSurfaceFit(
    controller: VideoPlayerController,
) {
    val size = controller.videoSize
    val ratio = if (size != IntSize.Zero && size.height > 0) {
        size.width.toFloat() / size.height
    } else {
        null
    }
    VideoSurface(
        controller = controller,
        modifier = if (ratio != null) {
            // aspectRatio 在约束内先试宽再试高，天然就是 fitCenter
            Modifier.align(Alignment.Center).aspectRatio(ratio)
        } else {
            Modifier.fillMaxSize()
        },
    )
}

internal const val PLAYER_TICK_INTERVAL_MS = 100L
private const val RESUME_MIN_PROGRESS_MS = 5_000L
private const val RESUME_AUTO_HIDE_MS = 5_000L
