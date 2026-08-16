package com.yenaly.han1meviewer.logic

import com.yenaly.han1meviewer.logic.network.HUpdater
import com.yenaly.han1meviewer.logic.state.WebsiteState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * 应用更新检查。原本是 `NetworkRepo.getLatestVersion`，Step 6 把 `NetworkRepo` 上移
 * commonMain 时留了下来——它依赖的 [HUpdater] 要读 Firebase Remote Config，是 Android 专属；
 * 而且 iOS 走 App Store 更新，本来也用不上这套。
 */
object UpdateRepo {

    fun getLatestVersion(forceCheck: Boolean = true) = flow {
        emit(WebsiteState.Loading)
        val versionInfo = HUpdater.checkForUpdate(forceCheck)
        emit(WebsiteState.Success(versionInfo))
    }.catch { e ->
        when (e) {
            is CancellationException -> throw e
            else -> {
                e.printStackTrace()
                emit(WebsiteState.Error(e))
            }
        }
    }.flowOn(Dispatchers.IO)
}
