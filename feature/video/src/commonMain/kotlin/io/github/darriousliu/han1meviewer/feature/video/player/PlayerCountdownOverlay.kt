package io.github.darriousliu.han1meviewer.feature.video.player

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.storage.entity.HKeyframeEntity
import net.sergeych.sprintf.sprintf

/**
 * H 帧倒计时（`tv_timer`）。仅全屏；命中窗口 `0 until whenCountdownRemind`（毫秒）,
 * 取第一条命中的关键帧。样式照 HJzvdStd :1042-1079 的 spannable：
 * 开了 `showCommentWhenCountdown` 时先画 `#序号`（0.7 倍字号）+ 提示语（0.7 倍）换行；
 * 秒数粗体——剩余 ≥1s 显示整秒 +1，<1s 显示一位小数。
 */
@Composable
internal fun PlayerCountdownOverlay(
    controller: VideoPlayerController,
    tick: Long,
    hKeyframes: HKeyframeEntity?,
    modifier: Modifier = Modifier,
) {
    @Suppress("UNUSED_EXPRESSION") tick
    val keyframes = hKeyframes?.keyframes ?: return
    val position = controller.positionMs
    val remindWindowMs = Preferences.whenCountdownRemind
    val showComment = Preferences.showCommentWhenCountdown

    var text: AnnotatedString? = null
    for ((index, keyframe) in keyframes.withIndex()) {
        val interval = keyframe.position - position
        if (interval in 0 until remindWindowMs) {
            text = buildAnnotatedString {
                if (showComment) {
                    withStyle(SpanStyle(fontSize = 0.7.em)) {
                        append("#${index + 1}")
                        val prompt = keyframe.prompt
                        if (!prompt.isNullOrBlank()) {
                            append(" $prompt")
                        }
                    }
                    append("\n")
                }
                val timeLong = interval / 1_000L
                val timeText = if (timeLong >= 1) {
                    (timeLong + 1).toString()
                } else {
                    // mp_stools 的 %.1f 无尾点 bug（%.0f 才有），固定用 `.` 不吃 locale
                    "%.1f".sprintf(interval / 1_000F)
                }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(timeText)
                }
            }
            break
        }
    }

    text?.let {
        Text(
            text = it,
            color = PlayerWhite,
            fontSize = 24.sp,
            modifier = modifier,
        )
    }
}
