package io.github.darriousliu.han1meviewer.core.common.util

import io.github.darriousliu.han1meviewer.core.common.BuildConfig

/**
 * 版本号形如 `1.0.2+30`，`+` 后面才是真正用来比大小的 versionCode。
 *
 * 拿不到（没有 `+` 或者不是数字）时返回 [Int.MAX_VALUE]，也就是**当作有新版本**——
 * 宁可多提示一次，也不要因为 tag 写法变了就再也提示不了更新。
 */
fun checkNeedUpdate(versionName: String): Boolean {
    val latestVersionCode = versionName.substringAfter("+", "").toIntOrNull() ?: Int.MAX_VALUE
    return BuildConfig.VERSION_CODE < latestVersionCode
}
