package io.github.darriousliu.han1meviewer.ui.screen.video.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.darriousliu.han1meviewer.util.formatVideoTime
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.back
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_arrow_back_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_pause_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_play_arrow_24
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Media3 播放器的控件层。
 *
 * ⚠️ **本文件目前只是 Step 25-2 的最小可播版本**：表面 + 播放/暂停 + 进度条 + 返回。
 * 目的是先把「只有真机能回答的未知」一次试掉——surface 挂不挂得上、HLS 能不能放、
 * 代理通不通、position 报得准不准。
 *
 * 完整控件（顶栏/底栏/清晰度/倍速/手势/全屏/PiP/H 帧倒计时）在后续几批按
 * `layout_jzvd_with_speed.xml` 的结构补齐——**以现在线上那套 jzvd UI 为准**，
 * 不是以 `VideoPlayerUi.kt` 为准（那是更早的一次尝试，属于老代码）。
 */
@Composable
fun HanimeVideoPlayer(
    controller: VideoPlayerController,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(Color.Black)) {
        VideoSurface(controller, Modifier.fillMaxSize())

        if (controller.isBuffering) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }

        // 顶栏：返回 + 标题
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(Res.drawable.ic_baseline_arrow_back_24),
                    contentDescription = stringResource(Res.string.back),
                    tint = Color.White,
                )
            }
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        // 中央播放/暂停
        IconButton(
            onClick = { if (controller.isPlaying) controller.pause() else controller.play() },
            modifier = Modifier.align(Alignment.Center).size(64.dp),
        ) {
            // 用仓里自带的 drawable 而不是 material icons：commonMain 只有
            // material-icons-core（49 个），且 Step 24 已经定了「图标统一走自带 drawable」
            Icon(
                painter = painterResource(
                    if (controller.isPlaying) Res.drawable.ic_baseline_pause_24
                    else Res.drawable.ic_baseline_play_arrow_24
                ),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp),
            )
        }

        PlayerProgressBar(
            controller = controller,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/**
 * 进度条。
 *
 * 拖动时只预览不 seek，抬手才提交——和 `HJzvdStd` 的 `touchActionUp` 语义一致。
 * ⚠️ 所有除法都要先判 `durationMs > 0`：未就绪时时长是 0，除下去就是 NaN。
 */
@Composable
private fun PlayerProgressBar(
    controller: VideoPlayerController,
    modifier: Modifier = Modifier,
) {
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }
    var tick by remember { mutableStateOf(0) }

    // controller 的 position 不是 State（直接读 ExoPlayer），所以自己打点驱动重组
    LaunchedEffect(controller) {
        while (true) {
            delay(TICK_INTERVAL_MS)
            tick++
        }
    }

    @Suppress("UNUSED_EXPRESSION") tick
    val duration = controller.durationMs
    val position = controller.positionMs
    val fraction = if (duration > 0L) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(formatVideoTime(position), color = Color.White, style = MaterialTheme.typography.labelSmall)
        Slider(
            value = if (scrubbing) scrubValue else fraction,
            onValueChange = { scrubbing = true; scrubValue = it },
            onValueChangeFinished = {
                if (duration > 0L) controller.seekTo((scrubValue * duration).toLong())
                scrubbing = false
            },
            modifier = Modifier.weight(1f),
        )
        Text(formatVideoTime(duration), color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

/** 和 `HJzvdStd` 把 jzvd 的 300ms 改成 100ms 是同一个理由：H 帧倒计时要亚秒精度。 */
private const val TICK_INTERVAL_MS = 100L
