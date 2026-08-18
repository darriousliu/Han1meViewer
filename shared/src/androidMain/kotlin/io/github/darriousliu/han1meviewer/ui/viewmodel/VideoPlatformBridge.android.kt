package io.github.darriousliu.han1meviewer.ui.viewmodel

import io.github.darriousliu.han1meviewer.HCacheManager
import io.github.darriousliu.han1meviewer.logic.model.HanimeVideo
import io.github.darriousliu.han1meviewer.util.application
import io.github.darriousliu.han1meviewer.util.dpPx
import kotlinx.coroutines.flow.Flow

actual object VideoPlatformBridge {

    actual fun loadCachedVideo(videoCode: String): Flow<HanimeVideo?> =
        HCacheManager.loadHanimeVideoInfo(application, videoCode)

    actual fun defaultPlayerHeightPx(): Int = 250.dpPx
}
