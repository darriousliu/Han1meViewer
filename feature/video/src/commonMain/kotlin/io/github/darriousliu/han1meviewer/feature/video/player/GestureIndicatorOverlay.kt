package io.github.darriousliu.han1meviewer.feature.video.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.ic_player_brightness_24
import io.github.darriousliu.han1meviewer.core.resource.ic_player_fast_forward_24
import io.github.darriousliu.han1meviewer.core.resource.ic_player_volume_24
import io.github.darriousliu.han1meviewer.core.resource.player_brightness
import io.github.darriousliu.han1meviewer.core.resource.player_progress
import io.github.darriousliu.han1meviewer.core.resource.player_volume
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

enum class GestureIndicatorType { Brightness, Volume, Progress }

/**
 * 手势指示浮层：170×190dp 玻璃卡（半透明黑 + 渐变 + 描边 + 36dp 圆角）。
 * 从旧 `VideoPlayerUi.kt` 搬来并去掉 RenderEffect 模糊层——Compose 的
 * renderEffect 采样图层自身内容，对纯色块模糊是每帧一个多余 pass、视觉零差别。
 */
@Composable
internal fun GestureIndicatorOverlay(
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(width = 170.dp, height = 190.dp)
                    .clip(RoundedCornerShape(36.dp)),
            ) {
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.32f)))
                Box(
                    Modifier.matchParentSize().background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.White.copy(alpha = 0.04f),
                            )
                        )
                    )
                )
                Box(
                    Modifier.matchParentSize().border(
                        1.dp,
                        Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(36.dp),
                    )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(
                            when (type) {
                                GestureIndicatorType.Brightness -> Res.drawable.ic_player_brightness_24
                                GestureIndicatorType.Volume -> Res.drawable.ic_player_volume_24
                                GestureIndicatorType.Progress -> Res.drawable.ic_player_fast_forward_24
                            }
                        ),
                        contentDescription = null,
                        tint = PlayerWhite,
                        modifier = Modifier.size(42.dp),
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = stringResource(
                            when (type) {
                                GestureIndicatorType.Brightness -> Res.string.player_brightness
                                GestureIndicatorType.Volume -> Res.string.player_volume
                                GestureIndicatorType.Progress -> Res.string.player_progress
                            }
                        ),
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(18.dp))
                    LinearProgressIndicator(
                        progress = { percent.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(100)),
                        trackColor = Color.White.copy(alpha = 0.12f),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = text,
                        color = PlayerWhite,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }
        }
    }
}
