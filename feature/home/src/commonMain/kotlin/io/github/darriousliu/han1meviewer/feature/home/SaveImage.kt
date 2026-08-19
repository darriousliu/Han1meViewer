package io.github.darriousliu.han1meviewer.feature.home

/**
 * 下载远程图片并存入系统相册(公告图长按保存)。
 * Android 走 MediaStore;其余平台待实现(空实现,不弹提示)。
 */
expect suspend fun saveImageToGallery(imageUrl: String)
