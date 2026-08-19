package io.github.darriousliu.han1meviewer.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.serialization.NavBackStackSerializer

/*
 * nav3 的返回栈就是一个 `MutableList<HanimeRoute>`，导航动作即列表操作。
 * 这里放建栈入口和几个导航语义扩展。
 */

/**
 * 建返回栈。**别用 nav3 自带的 `rememberNavBackStack`**：那个 API 把元素序列化器写死成
 * `PolymorphicSerializer(NavKey::class)`，而 `NavKey` 是开放接口、kotlinx 无法自动多态，
 * 所以 1.2.0-alpha07 起它在 composition 里直接抛：
 *
 * > You must pass a `SavedStateConfiguration.serializersModule` configured to handle
 * > `NavKey` open polymorphism. Define it with: `polymorphic(NavKey::class) { ... }`
 *
 * 照它说的做就得手写一份 28 条的注册表，漏一条只有运行时才炸。
 *
 * 这里改成把元素类型收窄到 sealed 的 [HanimeRoute]，序列化器直接用 `HanimeRoute.serializer()`：
 * sealed 自动多态，新路由只要挂在 [HanimeRoute] 下就自动进表，一条注册都不用写，
 * 进程死亡后的返回栈恢复照旧。
 */
@Composable
fun rememberHanimeBackStack(vararg elements: HanimeRoute): NavBackStack<HanimeRoute> =
    rememberSerializable(
        *elements,
        serializer = NavBackStackSerializer(HanimeRoute.serializer()),
    ) { NavBackStack(*elements) }

/**
 * 替代 nav2 的 `navigateSafely`。
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
fun NavBackStack<HanimeRoute>.navigateSafely(key: HanimeRoute) {
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
fun NavBackStack<HanimeRoute>.popTo(key: HanimeRoute, inclusive: Boolean = false): Boolean {
    val index = indexOfLast { it == key }
    if (index < 0) return false
    val targetSize = if (inclusive) index else index + 1
    if (size <= targetSize) return false
    while (size > targetSize) removeAt(size - 1)
    return true
}
