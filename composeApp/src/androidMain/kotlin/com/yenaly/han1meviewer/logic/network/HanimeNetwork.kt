package com.yenaly.han1meviewer.logic.network

import com.yenaly.han1meviewer.GETCHU_BASE_URL
import com.yenaly.han1meviewer.HA1_GITHUB_API_URL
import com.yenaly.han1meviewer.HANIME_BASE_URL
import com.yenaly.han1meviewer.logic.network.service.GetchuService
import com.yenaly.han1meviewer.logic.network.service.HGitHubService
import com.yenaly.han1meviewer.logic.network.service.HanimeBaseService
import com.yenaly.han1meviewer.logic.network.service.HanimeCommentService
import com.yenaly.han1meviewer.logic.network.service.HanimeMyListService
import com.yenaly.han1meviewer.logic.network.service.HanimeSubscriptionService
import io.ktor.client.HttpClient

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/08 008 22:35
 */
object HanimeNetwork {
    private val initialSiteClient = ServiceCreator.createKtorClient(NetworkClientProfile.Site)
    private var replaceableSiteClient: HttpClient? = null
    private var getchuClient = ServiceCreator.createKtorClient(NetworkClientProfile.Getchu)
    private val githubClient = ServiceCreator.createKtorClient(NetworkClientProfile.GitHub)

    @Volatile
    var hanimeService = HanimeBaseService(initialSiteClient, HANIME_BASE_URL)
        private set

    @Volatile
    var githubService = HGitHubService(githubClient, HA1_GITHUB_API_URL)
        private set

    @Volatile
    var getchuService = GetchuService(getchuClient, GETCHU_BASE_URL)
        private set

    @Volatile
    var commentService = HanimeCommentService(initialSiteClient, HANIME_BASE_URL)
        private set

    @Volatile
    var myListService = HanimeMyListService(initialSiteClient, HANIME_BASE_URL)
        private set

    @Volatile
    var subscriptionService = HanimeSubscriptionService(initialSiteClient, HANIME_BASE_URL)
        private set

    /**
     * Rebuilds exactly the four services rebuilt by the Retrofit implementation.
     * Subscription and GitHub deliberately retain their initial clients.
     */
    @Synchronized
    fun rebuildNetwork() {
        val newSiteClient = ServiceCreator.createKtorClient(NetworkClientProfile.Site)
        val newGetchuClient = ServiceCreator.createKtorClient(NetworkClientProfile.Getchu)

        try {
            val newHanimeService = HanimeBaseService(newSiteClient, HANIME_BASE_URL)
            val newGetchuService = GetchuService(newGetchuClient, GETCHU_BASE_URL)
            val newCommentService = HanimeCommentService(newSiteClient, HANIME_BASE_URL)
            val newMyListService = HanimeMyListService(newSiteClient, HANIME_BASE_URL)

            ServiceCreator.rebuildOkHttpClient()

            val oldReplaceableSiteClient = replaceableSiteClient
            val oldGetchuClient = getchuClient

            hanimeService = newHanimeService
            getchuService = newGetchuService
            commentService = newCommentService
            myListService = newMyListService
            replaceableSiteClient = newSiteClient
            getchuClient = newGetchuClient

            // The initial site client remains owned by subscriptionService. Only clients from
            // later rebuilds become replaceable and can be closed here.
            oldReplaceableSiteClient?.let { oldClient -> runCatching(oldClient::close) }
            runCatching(oldGetchuClient::close)
        } catch (throwable: Throwable) {
            runCatching(newSiteClient::close)
            runCatching(newGetchuClient::close)
            throw throwable
        }
    }
}
