package io.github.darriousliu.han1meviewer.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection

/** 转调 compose-ui 的 Android 互操作桥。 */
@Composable
actual fun rememberHostNestedScrollConnection(): NestedScrollConnection =
    rememberNestedScrollInteropConnection()
