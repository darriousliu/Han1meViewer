package com.yenaly.han1meviewer.logic.network.service

import com.yenaly.han1meviewer.logic.model.github.Artifacts
import com.yenaly.han1meviewer.logic.model.github.CommitComparison
import com.yenaly.han1meviewer.logic.model.github.Release
import com.yenaly.han1meviewer.logic.model.github.WorkflowRuns
import com.yenaly.han1meviewer.logic.network.NetworkJson
import com.yenaly.han1meviewer.logic.network.NetworkStatusException
import com.yenaly.han1meviewer.logic.network.StreamingNetworkResponse
import io.ktor.client.HttpClient
import kotlinx.serialization.decodeFromString

class HGitHubService(
    client: HttpClient,
    baseUrl: String,
) {
    private val transport = KtorServiceTransport(client, baseUrl)

    suspend fun getLatestVersion(): Release = getJson(pathSegments = listOf("releases", "latest"))

    suspend fun getWorkflowRuns(branch: String = "main"): WorkflowRuns = getJson(
        pathSegments = listOf("actions", "workflows", "ci.yml", "runs"),
    ) {
        url.parameters.append("event", "push")
        url.parameters.append("status", "success")
        url.parameters.append("per_page", "1")
        url.parameters.append("branch", branch)
    }

    suspend fun getCommitComparison(
        curSha: String,
        latestSha: String,
    ): CommitComparison = getJson(
        pathSegments = listOf("compare", "$curSha...$latestSha"),
    )

    suspend fun getArtifacts(url: String): Artifacts = getJson(urlOverride = url)

    suspend fun <T> request(
        url: String,
        consumer: suspend (StreamingNetworkResponse) -> T,
    ): T = transport.streaming(
        urlOverride = url,
        consumer = consumer,
    )

    private suspend inline fun <reified T> getJson(
        pathSegments: List<String> = emptyList(),
        urlOverride: String? = null,
        noinline configure: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response = transport.raw(
            pathSegments = pathSegments,
            urlOverride = urlOverride,
            configure = configure,
        )
        if (!response.isSuccessful) throw NetworkStatusException(response)
        return NetworkJson.decodeFromString((response.successBody ?: ByteArray(0)).decodeToString())
    }
}
