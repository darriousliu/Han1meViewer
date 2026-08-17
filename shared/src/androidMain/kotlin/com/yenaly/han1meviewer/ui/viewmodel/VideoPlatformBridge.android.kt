package com.yenaly.han1meviewer.ui.viewmodel

import com.yenaly.han1meviewer.HCacheManager
import com.yenaly.han1meviewer.logic.model.HanimeVideo
import com.yenaly.han1meviewer.util.application
import com.yenaly.han1meviewer.util.dpPx
import kotlinx.coroutines.flow.Flow

actual object VideoPlatformBridge {

    actual fun loadCachedVideo(videoCode: String): Flow<HanimeVideo?> =
        HCacheManager.loadHanimeVideoInfo(application, videoCode)

    actual fun defaultPlayerHeightPx(): Int = 250.dpPx
}
