package com.yenaly.han1meviewer.platform

import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

data class UpdateJobRequest(
    val version: String,
    val changelog: String,
    val downloadLink: String,
    val nodeId: String,
)

data class DownloadJobRequest(
    val quality: String?,
    val downloadUrl: String?,
    val videoType: String?,
    val hanimeName: String,
    val videoCode: String,
    val coverUrl: String,
)

/**
 * Platform-neutral boundary around the existing Android WorkManager orchestration.
 *
 * WorkRequest, WorkInfo, Worker arguments and Context deliberately remain implementation details.
 */
interface BackgroundJobScheduler {
    val runningDownloadCount: Flow<Int>

    fun pruneCompletedJobs(): PlatformActionResult<Unit>

    suspend fun initializeDownloads(): PlatformActionResult<Unit>

    fun enqueueUpdate(request: UpdateJobRequest): PlatformActionResult<Unit>

    /** Collects and handles update output using the platform's existing behavior. */
    suspend fun collectUpdateOutput(): PlatformActionResult<Unit>

    fun enqueueDownload(
        request: DownloadJobRequest,
        redownload: Boolean = false,
        waiting: Boolean = false,
    ): PlatformActionResult<Unit>

    fun resumeDownload(entity: HanimeDownloadEntity): PlatformActionResult<Unit>

    fun pauseDownload(entity: HanimeDownloadEntity): PlatformActionResult<Unit>

    fun deleteDownload(entity: HanimeDownloadEntity): PlatformActionResult<Unit>

    fun setMaxConcurrentDownloadCount(value: Int): PlatformActionResult<Unit>
}

expect fun backgroundJobScheduler(): BackgroundJobScheduler

internal object UnsupportedBackgroundJobScheduler : BackgroundJobScheduler {
    override val runningDownloadCount: Flow<Int> = flowOf(0)

    override fun pruneCompletedJobs(): PlatformActionResult<Unit> = unavailable()

    override suspend fun initializeDownloads(): PlatformActionResult<Unit> = unavailable()

    override fun enqueueUpdate(request: UpdateJobRequest): PlatformActionResult<Unit> = unavailable()

    override suspend fun collectUpdateOutput(): PlatformActionResult<Unit> = unavailable()

    override fun enqueueDownload(
        request: DownloadJobRequest,
        redownload: Boolean,
        waiting: Boolean,
    ): PlatformActionResult<Unit> = unavailable()

    override fun resumeDownload(entity: HanimeDownloadEntity): PlatformActionResult<Unit> =
        unavailable()

    override fun pauseDownload(entity: HanimeDownloadEntity): PlatformActionResult<Unit> =
        unavailable()

    override fun deleteDownload(entity: HanimeDownloadEntity): PlatformActionResult<Unit> =
        unavailable()

    override fun setMaxConcurrentDownloadCount(value: Int): PlatformActionResult<Unit> =
        unavailable()

    private fun unavailable(): PlatformActionResult<Nothing> =
        PlatformActionResult.Unavailable(UnavailableReason.UnsupportedPlatform)
}
