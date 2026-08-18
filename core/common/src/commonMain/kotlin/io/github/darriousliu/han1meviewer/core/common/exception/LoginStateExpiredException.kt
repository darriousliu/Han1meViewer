package io.github.darriousliu.han1meviewer.core.common.exception

import org.jetbrains.compose.resources.StringResource

class LoginStateExpiredException(
    override val messageRes: StringResource,
    devMessage: String = "login state expired",
) : IllegalStateException(devMessage), LocalizedException
