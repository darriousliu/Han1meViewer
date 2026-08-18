package io.github.darriousliu.han1meviewer.logic.exception

import org.jetbrains.compose.resources.StringResource

/**
 * IP被封鎖
 *
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2023/08/07 007 12:40
 */
class IPBlockedException(
    messageRes: StringResource,
    devMessage: String = "IP blocked",
) : CloudFlareBlockedException(messageRes, devMessage)
