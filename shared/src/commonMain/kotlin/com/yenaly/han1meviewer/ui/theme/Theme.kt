package com.yenaly.han1meviewer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import com.yenaly.han1meviewer.Preferences

/**
 * 平台的动态取色。只有 Android 12+ 有 Material You，其余平台返回 null，
 * 调用方回落到 [ThemeColorPreset.DEFAULT]。
 */
@Composable
expect fun dynamicColorSchemeOrNull(darkTheme: Boolean): ColorScheme?

@Composable
fun HanimeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when (val preset = ThemeColorPreset.fromKey(Preferences.themeColor)) {
        ThemeColorPreset.SYSTEM ->
            dynamicColorSchemeOrNull(darkTheme) ?: ThemeColorPreset.DEFAULT.colorScheme(darkTheme)

        else -> preset.colorScheme(darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content,
    )
}
