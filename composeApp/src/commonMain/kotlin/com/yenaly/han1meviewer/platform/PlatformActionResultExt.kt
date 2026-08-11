package com.yenaly.han1meviewer.platform

import kotlinx.coroutines.CancellationException

internal fun <T> PlatformActionResult<T>.getOrThrow(): T = when (this) {
    is PlatformActionResult.Success -> value
    PlatformActionResult.Cancelled -> throw CancellationException("Platform action was cancelled")
    is PlatformActionResult.Failure -> throw cause ?: IllegalStateException(message)
    is PlatformActionResult.Unavailable ->
        throw IllegalStateException("Platform action unavailable: $reason")
}
