package io.github.darriousliu.han1meviewer.core.common.util

import kotlinx.coroutines.CancellationException

/**
 * Run suspend catching
 *
 * 和 [runCatching] 的区别是不会把 [CancellationException] 吞掉——协程被取消时必须让它继续往上抛，
 * 否则父协程感知不到取消。
 *
 * @param block suspend block
 */
inline fun <R> runSuspendCatching(block: () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (c: CancellationException) {
        throw c
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
