package io.github.darriousliu.han1meviewer.util

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
import io.github.darriousliu.han1meviewer.core.common.FILE_PROVIDER_AUTHORITY
import io.github.darriousliu.han1meviewer.R
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
