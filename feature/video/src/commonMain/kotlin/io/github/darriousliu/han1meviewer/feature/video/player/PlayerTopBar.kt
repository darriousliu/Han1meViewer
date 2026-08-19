package io.github.darriousliu.han1meviewer.feature.video.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.back
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_arrow_back_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_home_24
import io.github.darriousliu.han1meviewer.core.resource.speed
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * 顶栏。对照 `layout_jzvd_with_speed.xml` 的 `layout_top`：
 * 60dp 高、黑→透明渐变（jz_title_bg 的等价）；标题 18sp、两行省略、仅全屏；
 * 右侧 50dp 高磨砂胶囊装 🥵 / 倍速 / 超分 / 电量+时间。
 * 非全屏只显示返回与回主页（`HJzvdStd.setScreenNormal`）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PlayerTopBar(
    title: String,
    isFullscreen: Boolean,
    showHKeyframeEntry: Boolean,
    speedLabel: String?,
    showSuperResolution: Boolean,
    superResolutionLabel: String,
    batteryPercent: Int?,
    timeText: String,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
    onHKeyframeClick: () -> Unit,
    onHKeyframeLongClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onSuperResolutionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent))
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 12.dp).size(32.dp)) {
            Icon(
                painter = painterResource(Res.drawable.ic_baseline_arrow_back_24),
                contentDescription = stringResource(Res.string.back),
                tint = PlayerWhite,
            )
        }
        IconButton(onClick = onGoHome, modifier = Modifier.padding(start = 12.dp).size(32.dp)) {
            Icon(
                painter = painterResource(Res.drawable.ic_baseline_home_24),
                contentDescription = null,
                tint = PlayerWhite,
            )
        }
        if (isFullscreen) {
            Text(
                text = title,
                color = PlayerWhite,
                fontSize = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (isFullscreen) {
            Row(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .height(50.dp)
                    .frostedCapsule()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showHKeyframeEntry) {
                    CapsuleTextItem(
                        text = "🥵",
                        modifier = Modifier
                            .width(42.dp)
                            .fillMaxHeight()
                            .combinedClickable(
                                onClick = onHKeyframeClick,
                                onLongClick = onHKeyframeLongClick,
                            ),
                    )
                }
                CapsuleTextItem(
                    text = speedLabel ?: stringResource(Res.string.speed),
                    modifier = Modifier
                        .width(42.dp)
                        .fillMaxHeight()
                        .clickable(onClick = onSpeedClick),
                )
                if (showSuperResolution) {
                    CapsuleTextItem(
                        text = superResolutionLabel,
                        modifier = Modifier
                            .width(48.dp)
                            .fillMaxHeight()
                            .clickable(onClick = onSuperResolutionClick),
                    )
                }
                if (batteryPercent != null) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        BatteryIndicator(percent = batteryPercent)
                        Text(text = timeText, color = PlayerWhite, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CapsuleTextItem(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = PlayerWhite,
            fontSize = 13.sp,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/** 23×10dp 电池：描边外壳 + 右侧触点 + 按电量填充。 */
@Composable
private fun BatteryIndicator(percent: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 23.dp, height = 10.dp)) {
        val capWidth = size.width * 0.06f
        val bodyWidth = size.width - capWidth
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        // 外壳
        drawRoundRect(
            color = PlayerWhite,
            size = Size(bodyWidth, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx()),
            style = stroke,
        )
        // 触点
        drawRect(
            color = PlayerWhite,
            topLeft = Offset(bodyWidth, size.height * 0.3f),
            size = Size(capWidth, size.height * 0.4f),
        )
        // 电量
        val inset = 2.dp.toPx()
        drawRect(
            color = PlayerWhite,
            topLeft = Offset(inset, inset),
            size = Size(
                (bodyWidth - inset * 2) * (percent.coerceIn(0, 100) / 100f),
                size.height - inset * 2,
            ),
        )
    }
}
