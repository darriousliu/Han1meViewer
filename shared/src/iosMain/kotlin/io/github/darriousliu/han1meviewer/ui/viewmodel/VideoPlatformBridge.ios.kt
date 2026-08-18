package io.github.darriousliu.han1meviewer.ui.viewmodel

import io.github.darriousliu.han1meviewer.logic.model.HanimeVideo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** 本平台还没有下载体系：无缓存可读，播放器高度用逻辑像素默认值。 */
actual object VideoPlatformBridge {

    actual fun loadCachedVideo(videoCode: String): Flow<HanimeVideo?> = flowOf(null)

    actual fun defaultPlayerHeightPx(): Int = 250
}
