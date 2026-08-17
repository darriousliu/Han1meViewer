package com.yenaly.han1meviewer.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/*
 * nav3 的返回栈就是一个 `MutableList<NavKey>`，导航动作即列表操作。
 * 这里放的是从 nav2 迁过来时需要保形的那几个语义。
 */

/**
 * 替代 nav2 的 `navigateSafely`（`NavControllerExt.kt`）。
 *
 * ⚠️ **守卫语义变了**：nav2 那版看的是
 * `currentBackStackEntry.lifecycle.currentState.isAtLeast(STARTED)`，
 * 即「转场动画还没走完就一律不放行」。nav3 的返回栈元素是纯数据、没有 Lifecycle，
 * 没有直接等价物，这里退化成「栈顶已经是同一个目的地就跳过」。
 *
 * 效果差别：连点**同一个**入口仍然只进一层（覆盖了绝大多数误触），
 * 但快速连点**两个不同**入口时两层都会进——nav2 那版会拦住第二个。
 * 官方推荐的精确方案是把点击回调包一层 `dropUnlessResumed`
 * （nav3 给每个 NavEntry 独立 LifecycleOwner），但那要求 lambda 定义在 entry 内部，
 * 而本项目的 `onNavigateToVideo` 等是在 entry 外层共享的，留待后续按需收紧。
 */
fun NavBackStack<NavKey>.navigateSafely(key: HanimeRoute) {
    if (lastOrNull() == key) return
    add(key)
}

/**
 * 替代 nav2 的 `popBackStack(route, inclusive)`。
 *
 * 一路弹到 [key] 为止：[inclusive] 为 true 时连 [key] 自己也弹掉。
 * 栈里找不到 [key]、或者已经在目标位置时返回 false 且不改动返回栈
 * （对齐 nav2 `popBackStack` 的 Boolean 语义）。
 */
fun NavBackStack<NavKey>.popTo(key: HanimeRoute, inclusive: Boolean = false): Boolean {
    val index = indexOfLast { it == key }
    if (index < 0) return false
    val targetSize = if (inclusive) index else index + 1
    if (size <= targetSize) return false
    while (size > targetSize) removeAt(size - 1)
    return true
}
