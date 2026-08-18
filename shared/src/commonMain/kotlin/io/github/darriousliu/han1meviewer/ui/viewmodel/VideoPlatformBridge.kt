package io.github.darriousliu.han1meviewer.ui.viewmodel

import io.github.darriousliu.han1meviewer.logic.model.HanimeVideo
import kotlinx.coroutines.flow.Flow

/**
 * 影片页仍需的平台能力。
 *
 * 原来是两个函数属性、由 `HanimeApplication.onCreate` 注入——lambda 注入已废弃
 * （赋值点难追踪、忘了注入只能靠运行时默认值兜底），改成 expect/actual：
 * 没有对应能力的平台在自己的 actual 里给出默认行为，编译期就闭合。
 */
expect object VideoPlatformBridge {

    /** 读本地下载缓存的视频信息；没有下载体系的平台返回空流。 */
    fun loadCachedVideo(videoCode: String): Flow<HanimeVideo?>

    /** 播放器默认高度（px）。 */
    fun defaultPlayerHeightPx(): Int
}
