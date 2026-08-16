package com.yenaly.han1meviewer.util

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.core.net.toUri
import com.yenaly.han1meviewer.FILE_PROVIDER_AUTHORITY
import com.yenaly.han1meviewer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

fun File.createFileIfNotExists(): Boolean = if (!exists()) {
    parentFile?.mkdirs()
    createNewFile()
} else {
    isFile
}

fun Drawable.saveTo(
    outputStream: OutputStream,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    quality: Int = 100,
): Boolean = try {
    outputStream.buffered().use { stream ->
        toBitmapOrNull()?.compress(format, quality, stream) == true
    }
} catch (e: Exception) {
    Log.w("Files", "Failed to write drawable", e)
    false
}

/**
 * Must be Activity Context!
 */
fun Context.openDownloadedHanimeVideoLocally(
    uri: String, onFileNotFound: (() -> Unit)? = null,
) {
    val videoUri = uri.toUri()
    if (videoUri.scheme == ContentResolver.SCHEME_CONTENT) {
        val resolver = contentResolver
        try {
            resolver.openFileDescriptor(videoUri, "r")?.use { pfd ->
                if (pfd.statSize <= 0) {
                    onFileNotFound?.invoke()
                    return
                }
            }
        } catch (_: Exception) {
            onFileNotFound?.invoke()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(videoUri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showShortToast(R.string.action_not_support)
            e.printStackTrace()
        }
    } else {
        val videoFile = File(videoUri.path ?: "")
        if (!videoFile.exists()) {
            onFileNotFound?.invoke()
            return
        }
        val fileUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, videoFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showShortToast(R.string.action_not_support)
            e.printStackTrace()
        }
    }
}

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
            Log.i("progress",bytesCopied.toString())
            bytesCopied
        }
    }
}
