package com.yenaly.han1meviewer.logic.exception

import org.jetbrains.compose.resources.StringResource

/**
 * 带本地化文案的异常。
 *
 * 数据层（`Parser` / `NetworkRepo` 这些）现在在 commonMain，拿不到 `applicationContext`
 * 也不该关心「当前语言」这种表现层概念，所以只存 [messageRes]，由 UI 层解析。
 * 和 `CheckInType.displayNameRes`、`SearchOption` 是同一套做法。
 *
 * ⚠️ 实现类**仍然要给 `message` 传一个值**——UI 有不少地方直接读 `throwable.message`
 * （`pienization`、Toast、日志）。约定是：`message` 放不本地化的开发用描述，
 * 给日志和崩溃上报看；[messageRes] 才是给用户看的。
 */
interface LocalizedException {
    val messageRes: StringResource
}
