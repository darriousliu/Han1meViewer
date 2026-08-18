package io.github.darriousliu.han1meviewer.ui.theme

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
 * Step 23 之前这里只有 `isSystemInDarkTheme()`，深色模式偏好是靠 Android 的
 * `AppCompatDelegate.setDefaultNightMode` 改 configuration 间接生效的——
 * 代价是每次切换都要重建 Activity，而且 desktop/iOS 上这个偏好完全不起作用。
 * 现在三端都直接观察偏好，切换即重组。
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
