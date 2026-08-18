package io.github.darriousliu.han1meviewer.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * API 28 起走 [ImageDecoder]：`setTargetSampleSize` 是**解码时**下采样，
 * 而且它按 EXIF 自动把方向转正。
 *
 * minSdk 是 27，那一个版本落到 [BitmapFactory] 的两遍解码：
 * 第一遍 `inJustDecodeBounds = true` 只读图片头拿尺寸不分配像素，第二遍带
 * `inSampleSize` 真解。这条路不读 EXIF——和迁移前 API < 28 走的
 * `MediaStore.Images.Media.getBitmap` 一样，不是回归。
 */
actual suspend fun decodeSampledImageBitmap(
    bytes: ByteArray,
    maxDimension: Int,
): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.setTargetSampleSize(
                    computeSampleSize(info.size.width, info.size.height, maxDimension)
                )
                // 默认可能给 HARDWARE bitmap，那种拿不到像素，后面裁剪要读
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        }
    }.getOrNull()?.asImageBitmap()
}

actual suspend fun encodeJpeg(bitmap: ImageBitmap, quality: Int): ByteArray? =
    withContext(Dispatchers.IO) {
        runCatching {
            ByteArrayOutputStream().use { out ->
                bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.toByteArray()
            }
        }.getOrNull()
    }
