package io.github.darriousliu.han1meviewer.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.darriousliu.han1meviewer.core.storage.Preferences

/**
 * 平台的动态取色。只有 Android 12+ 有 Material You，其余平台返回 null，
 * 调用方回落到 [ThemeColorPreset.DEFAULT]。
 */
@Composable
expect fun dynamicColorSchemeOrNull(darkTheme: Boolean): ColorScheme?

/**
 * 从 [Preferences.useDarkMode] 解出「现在该不该用深色」。
 *
 * 三端都直接观察偏好，切换即重组。不要换回 Android 的
 * `AppCompatDelegate.setDefaultNightMode`——那条路每次切换要重建 Activity，
 * 且 desktop/iOS 上完全不生效。
 */
@Composable
fun rememberIsDarkTheme(): Boolean {
    val mode by Preferences.useDarkModeStateFlow.collectAsState()
    return when (mode) {
        "always_on" -> true
        "always_off" -> false
        else -> isSystemInDarkTheme()   // follow_system
    }
}

@Composable
fun HanimeTheme(
    darkTheme: Boolean = rememberIsDarkTheme(),
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
