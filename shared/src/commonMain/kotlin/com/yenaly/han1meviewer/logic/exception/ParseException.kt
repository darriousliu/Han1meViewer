package com.yenaly.han1meviewer.logic.exception

import han1meviewer.shared.generated.resources.Res
import han1meviewer.shared.generated.resources.parse_error_msg
import org.jetbrains.compose.resources.StringResource

/**
 * 解析錯誤
 *
 * 文案对用户永远是同一句「解析失败」，所以 [messageRes] 写死；
 * `message` 里带上具体是哪个函数的哪个字段挂了，方便查日志。
 *
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2023/08/05 005 16:20
 */
class ParseException : RuntimeException, LocalizedException {

    override val messageRes: StringResource get() = Res.string.parse_error_msg

    constructor(
        funcName: String,
        varName: String,
    ) : super("[Parse::$funcName => $varName] parse error!")

    constructor(reason: String) : super(reason)
}
