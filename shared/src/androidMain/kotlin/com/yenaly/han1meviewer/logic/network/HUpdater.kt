package com.yenaly.han1meviewer.logic.network

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.FirebaseConstants
import com.yenaly.han1meviewer.HA1_GITHUB_DEFAULT_BRANCH
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.model.github.CommitComparison
import com.yenaly.han1meviewer.logic.model.github.Latest
import com.yenaly.han1meviewer.util.checkNeedUpdate
import com.yenaly.han1meviewer.util.copyTo
import com.yenaly.han1meviewer.util.runSuspendCatching
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
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

    const val DEFAULT_BRANCH = HA1_GITHUB_DEFAULT_BRANCH

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
            if (Preferences.useCIUpdateChannel && Firebase.remoteConfig.getBoolean(FirebaseConstants.ENABLE_CI_UPDATE)) {
                val curSha = BuildConfig.COMMIT_SHA
                // 特殊情况下才用注释部分，一般情况下 branch 都是固定的，要不然多一次
                // request 会对我的 API Token 造成负担。
                // val apiReq = request(HA1_GITHUB_API_URL)
                // val branch = apiReq.body?.string()?.let(::JSONObject)?.getString("default_branch")
                //     ?: return null
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
     * 迁移前走的是 Retrofit `@Streaming` 的 `githubService.request(url)`。
     * Ktor 这边必须用 `prepareGet {}.execute {}`——直接 `get()` 会把整个 APK
     * 先读进内存（响应体默认是要缓存下来的）。
     *
     * @param url update url
     */
    suspend fun File.injectUpdate(url: String, progress: (suspend (Int, Long, Long) -> Unit)? = null) {
        HanimeNetwork.githubClient.prepareGet(url).execute { res ->
            val contentLength = res.contentLength() ?: -1L
            Log.i(TAG, "content length: $contentLength")
            res.bodyAsChannel().toInputStream().use { stream ->
                if (url.endsWith("zip")) {
                    Log.d(TAG, "Injecting update from zip ($url)")
                    ZipInputStream(stream).use { zip ->
                        zip.nextEntry
                        this.outputStream().use {
                            // 估摸着压缩率为0.56左右，稍微估算解压后大小，防止进度卡在100%时间过长
                            zip.copyTo(it, (contentLength * 1.79).toLong(), progress = progress)
                        }
                    }
                } else {
                    Log.d(TAG, "Injecting update from release ($url)")
                    this.outputStream().use {
                        stream.copyTo(it, contentLength, progress = progress)
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