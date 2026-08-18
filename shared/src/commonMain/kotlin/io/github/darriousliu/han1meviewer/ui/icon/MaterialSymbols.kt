package io.github.darriousliu.han1meviewer.ui.icon

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/*
 * KMP 版 material-icons-core（org.jetbrains.compose.material:material-icons-core:1.7.3）
 * 只有 49 个图标，这里补齐 commonMain 用得到但 core 里没有的那些。
 *
 * 来源是 Google Fonts 的 Material Symbols 矢量源码，下载方式：
 *
 *   curl --compressed -o <name>.kt \
 *     "https://fonts.gstatic.com/render/v1/Material+Symbols+Outlined/24dp/<name>.kt\
 *      ?var=opsz,wght,FILL,GRAD,ROND@24,400,<FILL>,0,50"
 *
 * 拿到的是包名 com.example.test 下的顶层 `val <name>: ImageVector`，
 * 改成 Icons 的扩展属性放进来即可——调用点写法不用变，只多一行 import。
 * 注意 curl 要加 --compressed，否则拿到的是 gzip 字节流。
 *
 * URL 里 FILL 是第三个数字：0 = 描边款，1 = 实心款。要和被替代的
 * Material Icons 原图标观感对齐，各图标用的值见下面每一项的注释。
 *
 * 以后再遇到 core 里没有的图标，照这个流程加。
 */

/** Material Symbols `calendar_month`，FILL=1（实心款）。 */
val Icons.Filled.CalendarMonth: ImageVector
  get() {
    if (_CalendarMonth != null) {
      return _CalendarMonth!!
    }
    _CalendarMonth =
      ImageVector.Builder(
          name = "calendar_month",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12f, 14f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(11f, 13.43f, 11f, 13f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(12f, 12f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(13f, 13f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(12f, 14f)
            close()
            moveTo(7.29f, 13.71f)
            quadTo(7f, 13.43f, 7f, 13f)
            reflectiveQuadTo(7.29f, 12.29f)
            reflectiveQuadTo(8f, 12f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(9f, 13f)
            reflectiveQuadTo(8.71f, 13.71f)
            reflectiveQuadTo(8f, 14f)
            quadTo(7.58f, 14f, 7.29f, 13.71f)
            close()
            moveTo(16f, 14f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(15f, 13.43f, 15f, 13f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(16f, 12f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(17f, 13f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(16f, 14f)
            close()
            moveToRelative(-4f, 4f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(11f, 17.43f, 11f, 17f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(12f, 16f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(13f, 17f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(12f, 18f)
            close()
            moveTo(7.29f, 17.71f)
            quadTo(7f, 17.43f, 7f, 17f)
            reflectiveQuadTo(7.29f, 16.29f)
            reflectiveQuadTo(8f, 16f)
            reflectiveQuadToRelative(0.71f, 0.29f)
            reflectiveQuadTo(9f, 17f)
            reflectiveQuadTo(8.71f, 17.71f)
            reflectiveQuadTo(8f, 18f)
            quadTo(7.58f, 18f, 7.29f, 17.71f)
            close()
            moveTo(16f, 18f)
            quadToRelative(-0.42f, 0f, -0.71f, -0.29f)
            quadTo(15f, 17.43f, 15f, 17f)
            reflectiveQuadToRelative(0.29f, -0.71f)
            reflectiveQuadTo(16f, 16f)
            quadToRelative(0.43f, 0f, 0.71f, 0.29f)
            reflectiveQuadTo(17f, 17f)
            reflectiveQuadToRelative(-0.29f, 0.71f)
            reflectiveQuadTo(16f, 18f)
            close()
            moveTo(5f, 22f)
            quadTo(4.18f, 22f, 3.59f, 21.41f)
            reflectiveQuadTo(3f, 20f)
            verticalLineTo(6f)
            quadTo(3f, 5.18f, 3.59f, 4.59f)
            reflectiveQuadTo(5f, 4f)
            horizontalLineTo(6f)
            verticalLineTo(2f)
            horizontalLineTo(8f)
            verticalLineTo(4f)
            horizontalLineToRelative(8f)
            verticalLineTo(2f)
            horizontalLineToRelative(2f)
            verticalLineTo(4f)
            horizontalLineToRelative(1f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(21f, 5.18f, 21f, 6f)
            verticalLineTo(20f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 22f)
            horizontalLineTo(5f)
            close()
            moveTo(5f, 20f)
            horizontalLineTo(19f)
            verticalLineTo(10f)
            horizontalLineTo(5f)
            verticalLineTo(20f)
            close()
          }
        }
        .build()
    return _CalendarMonth!!
  }

private var _CalendarMonth: ImageVector? = null

/** Material Symbols `open_in_new`。这个字形没有实心变体，FILL=0 和 1 拿到的矢量完全一样。 */
val Icons.AutoMirrored.Filled.OpenInNew: ImageVector
  get() {
    if (_OpenInNew != null) {
      return _OpenInNew!!
    }
    _OpenInNew =
      ImageVector.Builder(
          name = "open_in_new",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5f, 21f)
            quadTo(4.18f, 21f, 3.59f, 20.41f)
            reflectiveQuadTo(3f, 19f)
            verticalLineTo(5f)
            quadTo(3f, 4.17f, 3.59f, 3.59f)
            reflectiveQuadTo(5f, 3f)
            horizontalLineToRelative(7f)
            verticalLineTo(5f)
            horizontalLineTo(5f)
            verticalLineTo(19f)
            horizontalLineTo(19f)
            verticalLineTo(12f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(7f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(19f, 21f)
            horizontalLineTo(5f)
            close()
            moveTo(9.7f, 15.7f)
            lineTo(8.3f, 14.3f)
            lineTo(17.6f, 5f)
            horizontalLineTo(14f)
            verticalLineTo(3f)
            horizontalLineToRelative(7f)
            verticalLineToRelative(7f)
            horizontalLineTo(19f)
            verticalLineTo(6.4f)
            lineTo(9.7f, 15.7f)
            close()
          }
        }
        .build()
    return _OpenInNew!!
  }

private var _OpenInNew: ImageVector? = null

/** Material Symbols `play_circle`，FILL=0（描边款）。 */
val Icons.Filled.PlayCircleOutline: ImageVector
  get() {
    if (_PlayCircleOutline != null) {
      return _PlayCircleOutline!!
    }
    _PlayCircleOutline =
      ImageVector.Builder(
          name = "play_circle",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(9.5f, 16.5f)
            lineToRelative(7f, -4.5f)
            lineTo(9.5f, 7.5f)
            verticalLineToRelative(9f)
            close()
            moveTo(12f, 22f)
            quadTo(9.93f, 22f, 8.1f, 21.21f)
            quadTo(6.28f, 20.43f, 4.93f, 19.08f)
            quadTo(3.58f, 17.73f, 2.79f, 15.9f)
            reflectiveQuadTo(2f, 12f)
            quadTo(2f, 9.92f, 2.79f, 8.1f)
            quadTo(3.58f, 6.27f, 4.93f, 4.93f)
            quadTo(6.28f, 3.57f, 8.1f, 2.79f)
            quadTo(9.93f, 2f, 12f, 2f)
            reflectiveQuadToRelative(3.9f, 0.79f)
            reflectiveQuadToRelative(3.17f, 2.14f)
            quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
            quadTo(22f, 9.92f, 22f, 12f)
            reflectiveQuadToRelative(-0.79f, 3.9f)
            reflectiveQuadToRelative(-2.14f, 3.17f)
            quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
            reflectiveQuadTo(12f, 22f)
            close()
            moveToRelative(0f, -2f)
            quadToRelative(3.35f, 0f, 5.68f, -2.32f)
            reflectiveQuadTo(20f, 12f)
            reflectiveQuadTo(17.68f, 6.32f)
            reflectiveQuadTo(12f, 4f)
            reflectiveQuadTo(6.33f, 6.32f)
            reflectiveQuadTo(4f, 12f)
            reflectiveQuadToRelative(2.33f, 5.68f)
            reflectiveQuadTo(12f, 20f)
            close()
            moveToRelative(0f, -8f)
            close()
          }
        }
        .build()
    return _PlayCircleOutline!!
  }

private var _PlayCircleOutline: ImageVector? = null

/** Material Symbols `drive_file_move`，FILL=1（实心款）。 */
val Icons.AutoMirrored.Filled.DriveFileMove: ImageVector
  get() {
    if (_DriveFileMove != null) {
      return _DriveFileMove!!
    }
    _DriveFileMove =
      ImageVector.Builder(
          name = "drive_file_move",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(4f, 20f)
            quadTo(3.18f, 20f, 2.59f, 19.41f)
            reflectiveQuadTo(2f, 18f)
            verticalLineTo(6f)
            quadTo(2f, 5.18f, 2.59f, 4.59f)
            reflectiveQuadTo(4f, 4f)
            horizontalLineToRelative(6f)
            lineToRelative(2f, 2f)
            horizontalLineToRelative(8f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(22f, 7.18f, 22f, 8f)
            verticalLineTo(18f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(20f, 20f)
            horizontalLineTo(4f)
            close()
            moveToRelative(8.2f, -6f)
            lineToRelative(-1.63f, 1.63f)
            lineToRelative(1.4f, 1.4f)
            lineTo(16f, 13f)
            lineTo(11.98f, 8.98f)
            lineToRelative(-1.4f, 1.4f)
            lineTo(12.2f, 12f)
            horizontalLineTo(8f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(4.2f)
            close()
          }
        }
        .build()
    return _DriveFileMove!!
  }

private var _DriveFileMove: ImageVector? = null

/** Material Symbols `remove`。纯减号横条，FILL 取值对它没有影响。 */
val Icons.Filled.Remove: ImageVector
  get() {
    if (_Remove != null) {
      return _Remove!!
    }
    _Remove =
      ImageVector.Builder(
          name = "remove",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(5f, 13f)
            verticalLineTo(11f)
            horizontalLineTo(19f)
            verticalLineToRelative(2f)
            horizontalLineTo(5f)
            close()
          }
        }
        .build()
    return _Remove!!
  }

private var _Remove: ImageVector? = null

/** Material Symbols `visibility`，FILL=1（实心款），对齐原 `Icons.Default.Visibility`。 */
val Icons.Filled.Visibility: ImageVector
  get() {
    if (_Visibility != null) {
      return _Visibility!!
    }
    _Visibility =
      ImageVector.Builder(
          name = "visibility",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(15.19f, 14.69f)
            quadTo(16.5f, 13.38f, 16.5f, 11.5f)
            reflectiveQuadTo(15.19f, 8.31f)
            reflectiveQuadTo(12f, 7f)
            reflectiveQuadTo(8.81f, 8.31f)
            reflectiveQuadTo(7.5f, 11.5f)
            reflectiveQuadToRelative(1.31f, 3.19f)
            reflectiveQuadTo(12f, 16f)
            reflectiveQuadToRelative(3.19f, -1.31f)
            close()
            moveToRelative(-5.1f, -1.28f)
            quadTo(9.3f, 12.63f, 9.3f, 11.5f)
            reflectiveQuadTo(10.09f, 9.59f)
            reflectiveQuadTo(12f, 8.8f)
            reflectiveQuadToRelative(1.91f, 0.79f)
            quadToRelative(0.79f, 0.79f, 0.79f, 1.91f)
            reflectiveQuadToRelative(-0.79f, 1.91f)
            reflectiveQuadTo(12f, 14.2f)
            reflectiveQuadTo(10.09f, 13.41f)
            close()
            moveTo(5.35f, 16.96f)
            quadTo(2.35f, 14.93f, 1f, 11.5f)
            quadTo(2.35f, 8.07f, 5.35f, 6.04f)
            reflectiveQuadTo(12f, 4f)
            reflectiveQuadToRelative(6.65f, 2.04f)
            reflectiveQuadTo(23f, 11.5f)
            quadToRelative(-1.35f, 3.42f, -4.35f, 5.46f)
            reflectiveQuadTo(12f, 19f)
            reflectiveQuadTo(5.35f, 16.96f)
            close()
          }
        }
        .build()
    return _Visibility!!
  }

private var _Visibility: ImageVector? = null

/** Material Symbols `visibility_off`，FILL=1（实心款），对齐原 `Icons.Default.VisibilityOff`。 */
val Icons.Filled.VisibilityOff: ImageVector
  get() {
    if (_VisibilityOff != null) {
      return _VisibilityOff!!
    }
    _VisibilityOff =
      ImageVector.Builder(
          name = "visibility_off",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(19.8f, 22.6f)
            lineTo(15.6f, 18.45f)
            quadToRelative(-0.88f, 0.28f, -1.76f, 0.41f)
            reflectiveQuadTo(12f, 19f)
            quadTo(8.23f, 19f, 5.28f, 16.91f)
            reflectiveQuadTo(1f, 11.5f)
            quadTo(1.53f, 10.17f, 2.33f, 9.04f)
            reflectiveQuadTo(4.15f, 7f)
            lineTo(1.4f, 4.2f)
            lineTo(2.8f, 2.8f)
            lineTo(21.2f, 21.2f)
            lineToRelative(-1.4f, 1.4f)
            close()
            moveTo(12f, 16f)
            quadToRelative(0.28f, 0f, 0.51f, -0.03f)
            reflectiveQuadToRelative(0.51f, -0.1f)
            lineToRelative(-5.4f, -5.4f)
            quadToRelative(-0.07f, 0.28f, -0.1f, 0.51f)
            quadTo(7.5f, 11.23f, 7.5f, 11.5f)
            quadToRelative(0f, 1.88f, 1.31f, 3.19f)
            reflectiveQuadTo(12f, 16f)
            close()
            moveToRelative(7.3f, 0.45f)
            lineTo(16.13f, 13.3f)
            quadTo(16.3f, 12.88f, 16.4f, 12.44f)
            reflectiveQuadTo(16.5f, 11.5f)
            quadToRelative(0f, -1.88f, -1.31f, -3.19f)
            reflectiveQuadTo(12f, 7f)
            quadTo(11.5f, 7f, 11.06f, 7.1f)
            reflectiveQuadTo(10.2f, 7.4f)
            lineTo(7.65f, 4.85f)
            quadTo(8.68f, 4.42f, 9.75f, 4.21f)
            reflectiveQuadTo(12f, 4f)
            quadToRelative(3.78f, 0f, 6.73f, 2.09f)
            reflectiveQuadTo(23f, 11.5f)
            quadToRelative(-0.57f, 1.47f, -1.51f, 2.74f)
            reflectiveQuadTo(19.3f, 16.45f)
            close()
            moveToRelative(-4.63f, -4.6f)
            lineToRelative(-3f, -3f)
            quadToRelative(0.7f, -0.13f, 1.29f, 0.11f)
            reflectiveQuadToRelative(1.01f, 0.69f)
            reflectiveQuadToRelative(0.61f, 1.04f)
            quadToRelative(0.19f, 0.59f, 0.09f, 1.16f)
            close()
          }
        }
        .build()
    return _VisibilityOff!!
  }

private var _VisibilityOff: ImageVector? = null
