package com.yenaly.han1meviewer.logic.network

import android.util.Log
import com.yenaly.han1meviewer.FirebaseConstants
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.model.github.CommitComparison
import com.yenaly.han1meviewer.logic.model.github.Latest
import com.yenaly.han1meviewer.platform.AppBuildInfoProvider
import com.yenaly.han1meviewer.platform.firebasePlatform
import com.yenaly.han1meviewer.util.checkNeedUpdate
import com.yenaly.han1meviewer.util.copyTo
import com.yenaly.han1meviewer.util.runSuspendCatching
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.File
import java.util.zip.ZipInputStream

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2024/03/21 021 08:28
 */
object HUpdater {

    const val TAG = "HUpdater"

    const val DEFAULT_BRANCH = "main"

    /**
     * Regex to match multiple line feeds to a single line feed
     */
    private val linefeedRegex = Regex("\\n{2,}")

    /**
     * Check for update
     *
     * @param forceCheck force check
     */
    suspend fun checkForUpdate(forceCheck: Boolean = false): Latest? {
        if (forceCheck || Preferences.isUpdateDialogVisible) {
            if (
                Preferences.useCIUpdateChannel &&
                firebasePlatform().remoteConfigBoolean(
                    FirebaseConstants.ENABLE_CI_UPDATE,
                    fallback = true,
                )
            ) {
                val curSha = AppBuildInfoProvider.current.commitSha
                // 默认分支固定，避免额外请求消耗 API Token。若需恢复动态查询，应通过统一的
                // kotlinx.serialization JSON 解析入口读取 default_branch。
                val workflowRun = HanimeNetwork.githubService.getWorkflowRuns()
                    .workflowRuns.firstOrNull() ?: return null
                val shortSha = workflowRun.headSha.take(7)
                if (shortSha != curSha) {
                    val artifacts =
                        HanimeNetwork.githubService.getArtifacts(workflowRun.artifactsUrl)
                    val archiveUrl = artifacts.downloadLink
                    val nodeId = artifacts.nodeId
                    val changelog = runSuspendCatching {
                        HanimeNetwork.githubService.getCommitComparison(
                            curSha = curSha,
                            latestSha = shortSha
                        ).commits.toChangelogPrettyString()
                    }.getOrNull() ?: workflowRun.title
                    return Latest("$shortSha (CI)", changelog, archiveUrl, nodeId)
                }
            } else {
                val ver = HanimeNetwork.githubService.getLatestVersion()
                val isNeeded = checkNeedUpdate(ver.tagName)
                if (isNeeded) {
                    return Latest(
                        ver.tagName, ver.body,
                        ver.assets.first().browserDownloadURL,
                        ver.assets.first().nodeID
                    )
                }
            }
        }
        return null
    }

    /**
     * Inject update to file
     *
     * @param url update url
     */
    suspend fun File.injectUpdate(url: String, progress: (suspend (Int, Long, Long) -> Unit)? = null) {
        HanimeNetwork.githubService.request(url) { response ->
            val contentLength = response.headerValues("Content-Length")
                .firstOrNull()
                ?.toLongOrNull()
                ?: -1L
            if (url.endsWith("zip")) {
                Log.d(TAG, "Injecting update from zip ($url)")
                response.successBody?.toInputStream()?.use { stream ->
                    ZipInputStream(stream).use { zip ->
                        zip.nextEntry
                        this.outputStream().use {
                            Log.i(TAG, "content length: $contentLength")
                            // 估摸着压缩率为0.56左右，稍微估算解压后大小，防止进度卡在100%时间过长
                            zip.copyTo(it, (contentLength * 1.79).toLong(), progress = progress)
                        }
                    }
                }
            } else {
                Log.d(TAG, "Injecting update from release ($url)")
                this.outputStream().use {
                    response.successBody?.toInputStream()?.use { body ->
                        Log.i(TAG, "content length: $contentLength")
                        body.copyTo(it, contentLength, progress = progress)
                    }
                }
            }
        }
    }

    /**
     * This function is used to filter out commits that are not authored by the user.
     */
    private val CommitComparison.Commit.CommitDetail.CommitAuthor.isAuthorShouldIgnore: Boolean
        get() = name.contains("dependabot")

    private fun List<CommitComparison.Commit>.toChangelogPrettyString(): String {
        return filterNot { commit ->
            commit.commit.author.isAuthorShouldIgnore
        }.distinct().reversed().joinToString("\n\n") { commit ->
            val message = commit.commit.message.replace(linefeedRegex, "\n")
            "↓ (@${commit.commit.author.name})\n$message"
        }
    }
}
