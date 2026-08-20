package io.github.darriousliu.han1meviewer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.graphics.drawable.toBitmapOrNull
import java.io.File
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
