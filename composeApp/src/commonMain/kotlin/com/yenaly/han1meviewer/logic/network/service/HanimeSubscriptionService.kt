package com.yenaly.han1meviewer.logic.network.service

import com.yenaly.han1meviewer.logic.network.RawNetworkResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters

class HanimeSubscriptionService(
    client: HttpClient,
    baseUrl: String,
) {
    private val transport = KtorServiceTransport(client, baseUrl)

    suspend fun subscribeArtist(
        csrfToken: String?,
        userId: String,
        artistId: String,
        status: String,
        csrfToken_1: String? = csrfToken,
    ): RawNetworkResponse = transport.raw(
        method = HttpMethod.Post,
        pathSegments = listOf("subscribe"),
    ) {
        setBody(FormDataContent(Parameters.build {
            csrfToken?.let { append("_token", it) }
            append("subscribe-user-id", userId)
            append("subscribe-artist-id", artistId)
            append("subscribe-status", status)
        }))
        csrfToken_1?.let { header("X-CSRF-TOKEN", it) }
    }
}
