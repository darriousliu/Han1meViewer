package io.github.darriousliu.han1meviewer.feature.home

import android.content.ContentValues
import android.content.Context
import io.github.darriousliu.han1meviewer.core.common.util.applicationContext
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import io.github.darriousliu.han1meviewer.feature.home.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Android 10 及以上通过 [MediaStore] 写入公共图片目录，低版本直接写入 Pictures 目录。
 * 保存成功后在主线程 Toast 提示。
 */
actual suspend fun saveImageToGallery(imageUrl: String) {
    val context: Context = applicationContext
    val loader = SingletonImageLoader.get(context)
    val request = ImageRequest.Builder(context)
        .data(imageUrl)
        .build()
    val result = (loader.execute(request) as? SuccessResult)?.image
    val bitmap = result?.toBitmap() ?: return
    val filename = "IMG_${System.currentTimeMillis()}.jpg"
    val fos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
        val uri =
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let { context.contentResolver.openOutputStream(it) }
    } else {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            filename
        )
        withContext(Dispatchers.IO) {
            FileOutputStream(file)
        }
    }
    fos?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
    withContext(Dispatchers.Main) {
        Toast.makeText(context, context.getString(R.string.saved), Toast.LENGTH_SHORT).show()
    }
}
