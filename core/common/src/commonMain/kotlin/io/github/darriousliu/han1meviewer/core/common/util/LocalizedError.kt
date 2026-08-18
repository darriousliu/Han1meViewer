package io.github.darriousliu.han1meviewer.core.common.util

import androidx.compose.runtime.Composable
import io.github.darriousliu.han1meviewer.core.common.exception.LocalizedException
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * 把异常转成给用户看的文案。
 *
 * 数据层的异常只带 [LocalizedException.messageRes]（见那个接口的注释），
 * 真正的解析在这里做。其它异常（系统的 IO/SSL 之类）回落到 `message`。
 */
@Composable
fun Throwable.localizedText(): String =
    if (this is LocalizedException) stringResource(messageRes) else message.orEmpty()

/**
 * [localizedText] 的挂起版，给 Toast、通知这种非 Composable 场景用。
 */
suspend fun Throwable.localizedTextOrNull(): String? =
    if (this is LocalizedException) getString(messageRes) else message
