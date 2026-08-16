package com.yenaly.han1meviewer.logic.exception

import org.jetbrains.compose.resources.StringResource

/**
 * 没有专门异常类型、但需要给用户一句本地化提示的场合（登录失败、未登录之类）。
 *
 * 继续继承 [IllegalStateException]：`LoginActivity` 是按 `is IllegalStateException`
 * 分支判断登录失败的，换成别的父类会悄悄改掉那里的行为。
 */
class LocalizedStateException(
    override val messageRes: StringResource,
    devMessage: String = "localized state error",
) : IllegalStateException(devMessage), LocalizedException
