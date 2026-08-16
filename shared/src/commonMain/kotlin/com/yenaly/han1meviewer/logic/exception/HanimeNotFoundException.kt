package com.yenaly.han1meviewer.logic.exception

import org.jetbrains.compose.resources.StringResource

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2023/08/07 007 13:08
 */
class HanimeNotFoundException(
    override val messageRes: StringResource,
    devMessage: String = "hanime not found",
) : RuntimeException(devMessage), LocalizedException
