package io.github.darriousliu.han1meviewer.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection

/** 原样转调 compose-ui 的 Android 互操作桥，行为与搬迁前逐字相同。 */
@Composable
actual fun rememberHostNestedScrollConnection(): NestedScrollConnection =
    rememberNestedScrollInteropConnection()
