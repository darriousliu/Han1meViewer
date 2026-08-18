package io.github.darriousliu.han1meviewer.util

import androidx.compose.ui.graphics.ImageBitmap

/**
 * 图片的采样解码与编码。**唯一存在的理由是「不能因为一张大图 OOM」**。
 *
 * 迁移前的头像裁剪页（`AvatarCropScreen`）直接 `ImageDecoder.decodeBitmap(source)`
 * 全量解码，再 `.copy(ARGB_8888, true)` 复制一份。一张 8000×6000 的相机原片
 * 解成 ARGB_8888 是 **183 MB**，加上那次 copy 峰值 **366 MB**——必炸。
 *
 * 现成库都不解决这件事，逐个查过：
 * - `FileKit.compressImage`（带 maxWidth/maxHeight，最像答案的一个）：Android actual
 *   第一行就是无 `inSampleSize` 的 `BitmapFactory.decodeByteArray`，全量解码完才缩放
 * - CMP commonMain 的 `ByteArray.decodeToImageBitmap()`：同样没有采样参数
 * - `cn.mucute:compose-avatar-cropper`（被本文件替换掉的那个）：解码根本在调用方
 *
 * 所以采样必须发生在**解码器内部**，只能各平台自己来。
 */

/** 预览解码的尺寸上限。2048² 的 ARGB_8888 最坏 16 MB，安全且足够裁剪取景。 */
const val PREVIEW_MAX_DIMENSION = 2048

/** 头像输出的尺寸上限。 */
const val AVATAR_MAX_DIMENSION = 1024

/** 头像 JPEG 画质，与迁移前一致。 */
const val AVATAR_JPEG_QUALITY = 90

/**
 * 算出使 `max(宽, 高) / 采样率 <= [maxDimension]` 的最小 2 的幂次采样率。
 *
 * 各平台的解码器（Android `inSampleSize` / `setTargetSampleSize`、
 * JDK `ImageReadParam.setSourceSubsampling`）都按整数倍抽行抽列，
 * 取 2 的幂次是为了让抽样落在字节边界上、各平台行为一致。
 *
 * 尺寸非法（<= 0，读不到图片头时会这样）时返回 1，让调用方去处理解码失败。
 */
fun computeSampleSize(srcWidth: Int, srcHeight: Int, maxDimension: Int): Int {
    if (srcWidth <= 0 || srcHeight <= 0 || maxDimension <= 0) return 1
    val longest = maxOf(srcWidth, srcHeight)
    var sample = 1
    while (longest / sample > maxDimension) sample = sample shl 1
    return sample
}

/**
 * 采样解码：先只读图片头拿到原始尺寸（**不分配像素**），算出采样率后再解码，
 * 保证结果最长边不超过 [maxDimension]。
 *
 * 解不出来（格式不支持、数据损坏）返回 null，不抛异常。
 *
 * 各平台都会顺带按 EXIF 把方向转正，唯一例外是 Android API 27
 * （`ImageDecoder` 是 API 28 起，minSdk 是 27）——那条路和迁移前的
 * `MediaStore.Images.Media.getBitmap` 行为一致，不是回归。
 */
expect suspend fun decodeSampledImageBitmap(bytes: ByteArray, maxDimension: Int): ImageBitmap?

/** [bitmap] 编码成 JPEG 字节。编码失败返回 null。 */
expect suspend fun encodeJpeg(bitmap: ImageBitmap, quality: Int): ByteArray?
