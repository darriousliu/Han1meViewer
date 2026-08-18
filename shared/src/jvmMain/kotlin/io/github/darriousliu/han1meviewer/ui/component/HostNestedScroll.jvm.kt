package io.github.darriousliu.han1meviewer.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection

/** 没有 View 壳要对接，全默认实现＝不拦截任何滚动。 */
@Composable
actual fun rememberHostNestedScrollConnection(): NestedScrollConnection =
    remember { object : NestedScrollConnection {} }
