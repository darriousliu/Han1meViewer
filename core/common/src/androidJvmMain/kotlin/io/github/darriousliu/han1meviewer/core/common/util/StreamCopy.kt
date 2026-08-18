package io.github.darriousliu.han1meviewer.core.common.util

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

private val logger = Logger.withTag("StreamCopy")

/**
 * copyTo with progress
 */
suspend fun InputStream.copyTo(
    out: OutputStream,
    contentLength: Long,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    progress: (suspend (Int, Long, Long) -> Unit)? = null,
): Long {
    return withContext(Dispatchers.IO) {
        this@copyTo.use {
            var bytesCopied: Long = 0
            val buffer = ByteArray(bufferSize)
            var bytes = read(buffer)
            var percent = 0
            while (bytes >= 0) {
                ensureActive()
                out.write(buffer, 0, bytes)
                bytesCopied += bytes
                if (contentLength > 0) {
                    val newPercent = (bytesCopied * 100 / contentLength).toInt()
                    if (newPercent != percent) {
                        percent = newPercent
                        progress?.invoke(percent.coerceAtMost(100), contentLength, bytesCopied)
                    }
                }
                bytes = read(buffer)
            }
            logger.i { bytesCopied.toString() }
            bytesCopied
        }
    }
}
