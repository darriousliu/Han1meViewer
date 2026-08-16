package com.yenaly.han1meviewer

/**
 * 代理类型。原本定义在 androidMain 的 `HProxySelector` 伴生对象里，
 * 为了让 [Preferences] 能进 commonMain 抽了出来。
 *
 * `HProxySelector.TYPE_*` 保留为指向这里的别名，现有调用点不受影响。
 */
object ProxyType {
    const val DIRECT = 0
    const val SYSTEM = 1
    const val HTTP = 2
    const val SOCKS = 3
}
