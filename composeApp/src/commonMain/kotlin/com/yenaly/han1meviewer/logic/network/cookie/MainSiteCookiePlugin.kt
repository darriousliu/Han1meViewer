package com.yenaly.han1meviewer.logic.network.cookie

import io.ktor.client.plugins.api.SendingRequest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders

/**
 * Ktor cookie plugin with HCookieJar-compatible batch replacement semantics.
 *
 * Install this only on the main-site client. In particular, do not combine it with Ktor's
 * HttpCookies plugin: HttpCookies saves each Set-Cookie independently and cannot preserve the
 * legacy whole-response replacement behavior.
 */
val MainSiteCookiePlugin = createClientPlugin("MainSiteCookiePlugin") {
    // SendingRequest also runs for redirected/retried sends, unlike the higher-level onRequest.
    on(SendingRequest) { request, _ ->
        val cookieHeader = MainSiteCookieRepository.cookieHeaderFor(request.url.build().host)
        if (cookieHeader == null) {
            request.headers.remove(HttpHeaders.Cookie)
        } else {
            request.headers[HttpHeaders.Cookie] = cookieHeader
        }
    }

    onResponse { response ->
        val setCookieHeaders = response.headers.getAll(HttpHeaders.SetCookie).orEmpty()
        if (setCookieHeaders.isNotEmpty()) {
            MainSiteCookieRepository.replaceFromResponse(
                host = response.call.request.url.host,
                setCookieHeaders = setCookieHeaders,
            )
        }
    }
}
