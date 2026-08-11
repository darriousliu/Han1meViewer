package com.yenaly.han1meviewer.ui.screen.home.dailycheckin

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 打卡页面使用的自包含矢量图标。
 *
 * 路径与原 Material filled 图标一致，避免依赖已停止更新的
 * Compose Material Icons Extended 组件。
 */
internal object DailyCheckInIcons {
    val ArrowBack: ImageVector by lazy {
        materialIcon("ArrowBack", autoMirror = true) {
            moveTo(20.0f, 11.0f)
            horizontalLineTo(7.83f)
            lineToRelative(5.59f, -5.59f)
            lineTo(12.0f, 4.0f)
            lineToRelative(-8.0f, 8.0f)
            lineToRelative(8.0f, 8.0f)
            lineToRelative(1.41f, -1.41f)
            lineTo(7.83f, 13.0f)
            horizontalLineTo(20.0f)
            verticalLineToRelative(-2.0f)
            close()
        }
    }

    val Check: ImageVector by lazy {
        materialIcon("Check") {
            moveTo(9.0f, 16.17f)
            lineTo(4.83f, 12.0f)
            lineToRelative(-1.42f, 1.41f)
            lineTo(9.0f, 19.0f)
            lineTo(21.0f, 7.0f)
            lineToRelative(-1.41f, -1.41f)
            close()
        }
    }

    val Close: ImageVector by lazy {
        materialIcon("Close") {
            moveTo(19.0f, 6.41f)
            lineTo(17.59f, 5.0f)
            lineTo(12.0f, 10.59f)
            lineTo(6.41f, 5.0f)
            lineTo(5.0f, 6.41f)
            lineTo(10.59f, 12.0f)
            lineTo(5.0f, 17.59f)
            lineTo(6.41f, 19.0f)
            lineTo(12.0f, 13.41f)
            lineTo(17.59f, 19.0f)
            lineTo(19.0f, 17.59f)
            lineTo(13.41f, 12.0f)
            close()
        }
    }

    val DateRange: ImageVector by lazy {
        materialIcon("DateRange") {
            moveTo(9.0f, 11.0f)
            lineTo(7.0f, 11.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(-2.0f)
            close()
            moveTo(13.0f, 11.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(-2.0f)
            close()
            moveTo(17.0f, 11.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(-2.0f)
            close()
            moveTo(19.0f, 4.0f)
            horizontalLineToRelative(-1.0f)
            lineTo(18.0f, 2.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(2.0f)
            lineTo(8.0f, 4.0f)
            lineTo(8.0f, 2.0f)
            lineTo(6.0f, 2.0f)
            verticalLineToRelative(2.0f)
            lineTo(5.0f, 4.0f)
            curveToRelative(-1.11f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
            lineTo(3.0f, 20.0f)
            curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            lineTo(21.0f, 6.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(19.0f, 20.0f)
            lineTo(5.0f, 20.0f)
            lineTo(5.0f, 9.0f)
            horizontalLineToRelative(14.0f)
            verticalLineToRelative(11.0f)
            close()
        }
    }

    val Favorite: ImageVector by lazy {
        materialIcon("Favorite") {
            moveTo(12.0f, 21.35f)
            lineToRelative(-1.45f, -1.32f)
            curveTo(5.4f, 15.36f, 2.0f, 12.28f, 2.0f, 8.5f)
            curveTo(2.0f, 5.42f, 4.42f, 3.0f, 7.5f, 3.0f)
            curveToRelative(1.74f, 0.0f, 3.41f, 0.81f, 4.5f, 2.09f)
            curveTo(13.09f, 3.81f, 14.76f, 3.0f, 16.5f, 3.0f)
            curveTo(19.58f, 3.0f, 22.0f, 5.42f, 22.0f, 8.5f)
            curveToRelative(0.0f, 3.78f, -3.4f, 6.86f, -8.55f, 11.54f)
            lineTo(12.0f, 21.35f)
            close()
        }
    }

    val Star: ImageVector by lazy {
        materialIcon("Star") {
            moveTo(12.0f, 17.27f)
            lineTo(18.18f, 21.0f)
            lineToRelative(-1.64f, -7.03f)
            lineTo(22.0f, 9.24f)
            lineToRelative(-7.19f, -0.61f)
            lineTo(12.0f, 2.0f)
            lineTo(9.19f, 8.63f)
            lineTo(2.0f, 9.24f)
            lineToRelative(5.46f, 4.73f)
            lineTo(5.82f, 21.0f)
            close()
        }
    }
}

private inline fun materialIcon(
    name: String,
    autoMirror: Boolean = false,
    pathBuilder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = "DailyCheckIn.$name",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24.0f,
    viewportHeight = 24.0f,
    autoMirror = autoMirror,
).apply {
    path(
        fill = SolidColor(Color.Black),
        pathBuilder = pathBuilder,
    )
}.build()
