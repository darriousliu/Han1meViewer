package com.yenaly.han1meviewer.logic.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

internal actual fun createPlatformNetworkClient(profile: NetworkClientProfile): HttpClient =
    HttpClient(Darwin) {
        configureCommonNetworkClient(
            profile = profile,
            // Darwin deliberately surfaces redirects so Ktor handles portable GET/HEAD redirects.
            followRedirectsInClient = true,
            installPortableSiteCookies = true,
        )
    }
