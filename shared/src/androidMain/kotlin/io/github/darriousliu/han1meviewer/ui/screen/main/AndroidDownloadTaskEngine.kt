package io.github.darriousliu.han1meviewer.ui.screen.main

import io.github.darriousliu.han1meviewer.core.storage.entity.download.HanimeDownloadEntity
import io.github.darriousliu.han1meviewer.feature.download.DownloadTaskEngine
import io.github.darriousliu.han1meviewer.worker.HanimeDownloadManagerV2

/**
 * [DownloadTaskEngine] 的 Android 实现,包 WorkManager 队列。
 * 引擎本体在 shared(与通知/worker 纠缠),feature:download 不能反依赖,
 * 所以经 `LocalDownloadTaskEngine` 由宿主组合根注入。
 */
object AndroidDownloadTaskEngine : DownloadTaskEngine {

    override fun pause(entity: HanimeDownloadEntity) {
        HanimeDownloadManagerV2.stopTask(entity)
    }

    override fun resume(entity: HanimeDownloadEntity) {
        HanimeDownloadManagerV2.resumeTask(entity)
    }

    override fun delete(entity: HanimeDownloadEntity) {
        HanimeDownloadManagerV2.deleteTask(entity)
    }

    override fun setMaxConcurrent(count: Int) {
        HanimeDownloadManagerV2.maxConcurrentDownloadCount = count
    }
}
