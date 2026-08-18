package com.yenaly.han1meviewer.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection

/**
 * 让内层可滚动列表的滚动**带动外层容器**。
 *
 * Android 上这是和 View 壳的互操作桥：视频页那几个 Compose 屏跑在一个 `ComposeView` 里，
 * 而它嵌在 `CoordinatorLayout` / `AppBarLayout` 中——没有这座桥，列表能滚但
 * **播放器区域不会跟着折叠**。
 *
 * 其余平台没有 View 壳，返回一个全默认实现（即不拦截任何滚动）即可。
 *
 * 等播放器那步把 View 壳干掉之后，Android 的 actual 也可以退化成空实现。
 */
@Composable
expect fun rememberHostNestedScrollConnection(): NestedScrollConnection
