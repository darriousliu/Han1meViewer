package io.github.darriousliu.han1meviewer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** iOS 没有 Material You，回落到预设配色。 */
@Composable
actual fun dynamicColorSchemeOrNull(darkTheme: Boolean): ColorScheme? = null
