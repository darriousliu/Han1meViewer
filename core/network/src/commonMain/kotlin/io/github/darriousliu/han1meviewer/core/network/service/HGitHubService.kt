package io.github.darriousliu.han1meviewer.core.network.service

import io.github.darriousliu.han1meviewer.core.common.HA1_GITHUB_DEFAULT_BRANCH
import io.github.darriousliu.han1meviewer.core.model.github.Artifacts
import io.github.darriousliu.han1meviewer.core.model.github.CommitComparison
import io.github.darriousliu.han1meviewer.core.model.github.Release
import io.github.darriousliu.han1meviewer.core.model.github.WorkflowRuns
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query
import de.jensklingenberg.ktorfit.http.Url

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/09/09 009 20:04
 */
interface HGitHubService {
    @GET("releases/latest")
    suspend fun getLatestVersion(): Release

    /**
     * What is Workflow Runs?
     *
     * List all workflow runs for a repository. You can use parameters to filter the list of results. For example, you
     * can get a list of workflow runs for a specific branch, or you can get a list of workflow runs that used a specific
     * workflow file.
     */
    @GET("actions/workflows/ci.yml/runs?event=push&status=success&per_page=1")
    suspend fun getWorkflowRuns(
        @Query("branch") branch: String = HA1_GITHUB_DEFAULT_BRANCH,
    ): WorkflowRuns

    /**
     * What is Commit Comparison?
     *
     * Compare two commits in a repository. The response will include a comparison of the two commits. The response can
     * include difference in various aspects such as files, commits, and comments.
     */
    @GET("compare/{curSha}...{latestSha}")
    suspend fun getCommitComparison(
        @Path("curSha") curSha: String,
        @Path("latestSha") latestSha: String,
    ): CommitComparison

    /**
     * What is Artifacts?
     *
     * Artifacts are the files produced by a workflow run. They are associated with the run during the execution of the
     * job that produces them. Artifacts are available for 90 days after the run is completed.
     */
    @GET
    suspend fun getArtifacts(
        @Url url: String,
    ): Artifacts

    // 更新包下载不放在这个接口里：走 HanimeNetwork.githubClient 的 prepareGet
    // （见 ApkUpdateDownloader），否则 Ktor 默认会把整个 APK 先读进内存。
}
