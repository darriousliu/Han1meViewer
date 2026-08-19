package io.github.darriousliu.han1meviewer.feature.video.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.darriousliu.han1meviewer.core.common.util.formatVideoTime
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.clarity
import io.github.darriousliu.han1meviewer.core.resource.ic_player_fullscreen_24
import io.github.darriousliu.han1meviewer.core.resource.ic_player_fullscreen_exit_24
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * 底栏。对照 `layout_jzvd_with_speed.xml` 的 `layout_bottom`：
 * 40dp 高的悬浮磨砂胶囊，距底 16dp、水平 margin 非全屏 15dp / 全屏 30dp
 * （原 `player_control_bar_margin` 的 values / values-land）、elevation 8dp。
 * 内容：`当前 | 总长` + 自绘 SeekBar + 画质文字（仅全屏）+ 全屏钮。
 */
@Composable
internal fun PlayerBottomBar(
    controller: VideoPlayerController,
    uiState: PlayerUiState,
    tick: Long,
    isFullscreen: Boolean,
    currentQuality: String?,
    onQualityClick: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_EXPRESSION") tick
    val duration = controller.durationMs
    val position = controller.positionMs
    val fraction = if (duration > 0L) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val buffered =
        if (duration > 0L) (controller.bufferedPositionMs.toFloat() / duration).coerceIn(0f, 1f)
        else 0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (isFullscreen) 30.dp else 15.dp,
                end = if (isFullscreen) 30.dp else 15.dp,
                bottom = 16.dp,
            )
            .height(40.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .frostedCapsule(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatVideoTime(if (uiState.scrubbing && duration > 0L) {
                (uiState.scrubFraction * duration).toLong()
            } else {
                position
            }),
            color = PlayerWhite,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 15.dp),
        )
        Text(
            text = " | ",
            color = PlayerWhite80,
            fontSize = 12.sp,
        )
        Text(
            text = formatVideoTime(duration),
            color = PlayerWhite,
            fontSize = 12.sp,
        )
        PlayerSeekBar(
            fraction = fraction,
            buffered = buffered,
            uiState = uiState,
            onSeekTo = { targetFraction ->
                if (duration > 0L) controller.seekTo((targetFraction * duration).toLong())
            },
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        if (isFullscreen) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable(onClick = onQualityClick)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = currentQuality ?: stringResource(Res.string.clarity),
                    color = PlayerWhite,
                    fontSize = 13.sp,
                )
            }
        }
        Box(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight()
                .clickable(onClick = onToggleFullscreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(
                    if (isFullscreen) Res.drawable.ic_player_fullscreen_exit_24
                    else Res.drawable.ic_player_fullscreen_24
                ),
                contentDescription = null,
                tint = PlayerWhite,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * 自绘轨道的 SeekBar：底轨 [SeekTrackBackground]、缓冲轨白、进度轨主题色、小白圆 thumb。
 * 自绘是为了避开 M3 Slider 默认的 minTouchTarget 把 40dp 胶囊撑高。
 * 拖动只预览、抬手才 seek（`HJzvdStd.touchActionUp` 语义）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSeekBar(
    fraction: Float,
    buffered: Float,
    uiState: PlayerUiState,
    onSeekTo: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Slider(
        value = if (uiState.scrubbing) uiState.scrubFraction else fraction,
        onValueChange = {
            uiState.scrubbing = true
            uiState.scrubFraction = it
        },
        onValueChangeFinished = {
            onSeekTo(uiState.scrubFraction)
            uiState.scrubbing = false
        },
        modifier = modifier.height(16.dp),
        thumb = {
            Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(10.dp).background(PlayerWhite, CircleShape))
            }
        },
        track = {
            Box(
                modifier = Modifier.fillMaxWidth().height(16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(100))
                        .background(SeekTrackBackground.copy(alpha = 0.35f))
                )
                Box(
                    Modifier
                        .fillMaxWidth(buffered)
                        .height(2.dp)
                        .clip(RoundedCornerShape(100))
                        .background(SeekTrackBackground)
                )
                Box(
                    Modifier
                        .fillMaxWidth(
                            (if (uiState.scrubbing) uiState.scrubFraction else fraction)
                                .coerceIn(0f, 1f)
                        )
                        .height(2.dp)
                        .clip(RoundedCornerShape(100))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        },
    )
}

/**
 * 独立于底栏的迷你进度条：1.5dp 高贴住播放器底边（`bottom_progress`），
 * 控件收起时显示；完播满格。
 */
@Composable
internal fun PlayerMiniProgressBar(
    controller: VideoPlayerController,
    tick: Long,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_EXPRESSION") tick
    val duration = controller.durationMs
    val fraction = when {
        controller.isEnded -> 1f
        duration > 0L -> (controller.positionMs.toFloat() / duration).coerceIn(0f, 1f)
        else -> 0f
    }
    val buffered =
        if (duration > 0L) (controller.bufferedPositionMs.toFloat() / duration).coerceIn(0f, 1f)
        else 0f
    Box(modifier = modifier.fillMaxWidth().height(1.5.dp)) {
        Box(
            Modifier
                .fillMaxWidth(buffered)
                .height(1.5.dp)
                .background(SeekTrackBackground)
        )
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(1.5.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
