package io.github.darriousliu.han1meviewer.feature.video.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * 播放器控件的共用样式常量。对照 `layout_jzvd_with_speed.xml` 与 frosted_glass.xml：
 * 磨砂 = 半透明灰底 + 圆角 + 半透明白描边。**不要用 RenderEffect 模糊**——
 * Compose 的 renderEffect 采样图层自身内容，对半透明色块做模糊是每帧一个
 * 多余渲染 pass、视觉零差别（旧 VideoPlayerUi.kt 踩过）。
 */

/** frosted_glass.xml 的底色。 */
internal val FrostedGlassColor = Color(0xA2545454)

internal val FrostedBorderColor = Color(0x33FFFFFF)

/** 控件文字/图标的主白色与 80% 白（锁图标 tint，对应 per80_transparent_white）。 */
internal val PlayerWhite = Color.White
internal val PlayerWhite80 = Color(0xCCFFFFFF)

/** 底部 SeekBar 的背景轨（bottom_seek_progress.xml 的 0xA5FFFFFF）。 */
internal val SeekTrackBackground = Color(0xA5FFFFFF)

internal fun Modifier.frostedCapsule(cornerRadius: Int = 20): Modifier {
    val shape = RoundedCornerShape(cornerRadius.dp)
    return background(FrostedGlassColor, shape)
        .border(1.dp, FrostedBorderColor, shape)
}
