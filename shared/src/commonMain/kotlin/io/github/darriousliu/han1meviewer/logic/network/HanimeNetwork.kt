package io.github.darriousliu.han1meviewer.logic.network

import io.github.darriousliu.han1meviewer.GETCHU_BASE_URL
import io.github.darriousliu.han1meviewer.HA1_GITHUB_API_URL
import io.github.darriousliu.han1meviewer.HANIME_BASE_URL
import io.github.darriousliu.han1meviewer.core.common.HJson
import io.github.darriousliu.han1meviewer.logic.network.service.GetchuService
import io.github.darriousliu.han1meviewer.logic.network.service.HGitHubService
import io.github.darriousliu.han1meviewer.logic.network.service.HanimeBaseService
import io.github.darriousliu.han1meviewer.logic.network.service.HanimeCommentService
import io.github.darriousliu.han1meviewer.logic.network.service.HanimeMyListService
import io.github.darriousliu.han1meviewer.logic.network.service.HanimeSubscriptionService
import io.github.darriousliu.han1meviewer.logic.network.service.createGetchuService
import io.github.darriousliu.han1meviewer.logic.network.service.createHGitHubService
import io.github.darriousliu.han1meviewer.logic.network.service.createHanimeBaseService
import io.github.darriousliu.han1meviewer.logic.network.service.createHanimeCommentService
import io.github.darriousliu.han1meviewer.logic.network.service.createHanimeMyListService
import io.github.darriousliu.han1meviewer.logic.network.service.createHanimeSubscriptionService
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

/**
 * 迁移前的 `ServiceCreator` + `HanimeNetwork`。
 *
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/08 008 22:35
 */
object HanimeNetwork {

    /** 主站 client。除了给 Ktorfit 用，连通性测试之类的临时请求也走它。 */
    var hClient: HttpClient = buildHttpClient(HClientSpec.HANIME)
        private set

    /**
     * getchu 的 client。除了 Ktorfit，getchu 图片的 coil3 ImageLoader 也用它
     * —— 那些图必须带 [HClientSpec.GETCHU] 里配的 Referer/Cookie 才能取到。
     */
    var getchuClient: HttpClient = buildHttpClient(HClientSpec.GETCHU)
        private set

    /**
     * GitHub API 的返回是 JSON，要装 ContentNegotiation 才能直接反序列化成 [HGitHubService]
     * 里那些 model。别的几个 client 拿到的都是 HTML，不需要。
     */
    var githubClient: HttpClient = buildGithubClient()
        private set

    /** 视频下载用。裸 client，不走 Ktorfit。 */
    var downloadClient: HttpClient = buildHttpClient(HClientSpec.DOWNLOAD)
        private set

    var hanimeService: HanimeBaseService = createHanimeService()
        private set
    var githubService: HGitHubService = createGithubService()
        private set
    var getchuService: GetchuService = createGetchuService()
        private set
    var commentService: HanimeCommentService = createCommentService()
        private set
    var myListService: HanimeMyListService = createMyListService()
        private set
    var subscriptionService: HanimeSubscriptionService = createSubscriptionService()
        private set

    /**
     * 用户改了镜像站/代理/DNS 之后重建。
     *
     * 迁移前只是把旧的 `OkHttpClient` 丢掉（连接池就那么挂着），现在显式 `close()`。
     * githubClient 和 downloadClient 不受这些设置影响，不用重建。
     */
    fun rebuildNetwork() {
        val oldH = hClient
        val oldGetchu = getchuClient
        hClient = buildHttpClient(HClientSpec.HANIME)
        getchuClient = buildHttpClient(HClientSpec.GETCHU)

        hanimeService = createHanimeService()
        getchuService = createGetchuService()
        commentService = createCommentService()
        myListService = createMyListService()
        subscriptionService = createSubscriptionService()

        oldH.close()
        oldGetchu.close()
    }

    private fun buildGithubClient(): HttpClient = buildHttpClient(HClientSpec.GITHUB).config {
        install(ContentNegotiation) { json(HJson) }
    }

    /** Ktorfit 要求 baseUrl 以 `/` 结尾。 */
    private fun ktorfit(baseUrl: String, client: HttpClient) = Ktorfit.Builder()
        .baseUrl(if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/")
        .httpClient(client)
        .build()

    private fun createHanimeService() =
        ktorfit(HANIME_BASE_URL, hClient).createHanimeBaseService()

    private fun createCommentService() =
        ktorfit(HANIME_BASE_URL, hClient).createHanimeCommentService()

    private fun createMyListService() =
        ktorfit(HANIME_BASE_URL, hClient).createHanimeMyListService()

    private fun createSubscriptionService() =
        ktorfit(HANIME_BASE_URL, hClient).createHanimeSubscriptionService()

    private fun createGetchuService() =
        ktorfit(GETCHU_BASE_URL, getchuClient).createGetchuService()

    private fun createGithubService() =
        ktorfit(HA1_GITHUB_API_URL, githubClient).createHGitHubService()
}
