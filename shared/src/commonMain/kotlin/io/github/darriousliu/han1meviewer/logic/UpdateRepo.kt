package io.github.darriousliu.han1meviewer.logic

import io.github.darriousliu.han1meviewer.logic.network.HUpdater
import io.github.darriousliu.han1meviewer.core.common.state.WebsiteState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * 应用更新检查。原本是 `NetworkRepo.getLatestVersion`。
 *
 * [HUpdater] 里唯一的平台差异（Firebase Remote Config 的 CI 渠道开关）已经收成一个
 * 注入点，检查逻辑本身是纯 GitHub API 调用，所以这层可以留在 commonMain。
 * 真正只有 Android 才有的是**下载安装包**那一步，见 androidMain 的 `injectUpdate`。
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
