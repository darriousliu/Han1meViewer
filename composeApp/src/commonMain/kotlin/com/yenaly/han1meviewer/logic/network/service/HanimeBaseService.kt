package com.yenaly.han1meviewer.logic.network.service

import com.yenaly.han1meviewer.logic.network.RawNetworkResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters

class HanimeBaseService(
    client: HttpClient,
    baseUrl: String,
) {
    private val transport = KtorServiceTransport(client, baseUrl)

    suspend fun getHomePage(url: String): RawNetworkResponse =
        transport.raw(urlOverride = url)

    suspend fun getHanimeSearchResult(
        page: Int = 1,
        query: String? = null,
        genre: String? = null,
        sort: String? = null,
        broad: String? = null,
        date: String? = null,
        duration: String? = null,
        tags: Set<String> = emptySet(),
        brands: Set<String> = emptySet(),
    ): RawNetworkResponse = transport.raw(pathSegments = listOf("search")) {
        url.parameters.append("page", page.toString())
        query?.let { url.parameters.append("query", it) }
        genre?.let { url.parameters.append("genre", it) }
        sort?.let { url.parameters.append("sort", it) }
        broad?.let { url.parameters.append("broad", it) }
        date?.let { url.parameters.append("date", it) }
        duration?.let { url.parameters.append("duration", it) }
        tags.forEach { url.parameters.append("tags[]", it) }
        brands.forEach { url.parameters.append("brands[]", it) }
    }

    suspend fun getHanimeVideo(videoCode: String): RawNetworkResponse =
        transport.raw(pathSegments = listOf("watch")) {
            url.parameters.append("v", videoCode)
        }

    suspend fun getHanimePreview(date: String): RawNetworkResponse =
        transport.raw(pathSegments = listOf("previews", date))

    suspend fun login(
        csrfToken: String?,
        email: String,
        password: String,
        csrfToken_1: String? = csrfToken,
    ): RawNetworkResponse = transport.raw(
        method = HttpMethod.Post,
        pathSegments = listOf("login"),
    ) {
        setBody(FormDataContent(Parameters.build {
            csrfToken?.let { append("_token", it) }
            append("email", email)
            append("password", password)
        }))
        csrfToken_1?.let { header("X-CSRF-TOKEN", it) }
    }

    suspend fun getLoginPage(): RawNetworkResponse =
        transport.raw(pathSegments = listOf("login"))

    suspend fun getMySubscriptions(page: Int): RawNetworkResponse =
        transport.raw(pathSegments = listOf("subscriptions")) {
            url.parameters.append("page", page.toString())
        }
}
