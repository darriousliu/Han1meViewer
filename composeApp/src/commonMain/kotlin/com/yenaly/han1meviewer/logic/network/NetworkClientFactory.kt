package com.yenaly.han1meviewer.logic.network

import com.yenaly.han1meviewer.logic.network.cookie.MainSiteCookiePlugin
import com.yenaly.han1meviewer.platform.AppBuildInfoProvider
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LoggingFormat
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal enum class NetworkClientProfile {
    Site,
    Getchu,
    GitHub,
}

internal val NetworkJson = Json {
    ignoreUnknownKeys = true
}

/** Creates a fresh, independently owned client. Callers must close clients they replace. */
internal expect fun createPlatformNetworkClient(profile: NetworkClientProfile): HttpClient

internal fun HttpClientConfig<*>.configureCommonNetworkClient(
    profile: NetworkClientProfile,
    followRedirectsInClient: Boolean,
    installPortableSiteCookies: Boolean,
) {
    expectSuccess = false
    followRedirects = followRedirectsInClient

    install(HttpTimeout) {
        // Retrofit's clients had no whole-call timeout, a 15 second connect timeout and
        // OkHttp's default 10 second read/write timeout.
        requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 10_000
    }
    install(ContentNegotiation) {
        json(NetworkJson)
    }
    install(Logging) {
        logger = SanitizedNetworkLogger
        level = LogLevel.INFO
        format = LoggingFormat.OkHttp
        sanitizeHeader { name ->
            SENSITIVE_HEADERS.any { sensitive -> sensitive.equals(name, ignoreCase = true) }
        }
    }
    defaultRequest {
        when (profile) {
            NetworkClientProfile.Site -> {
                headers.append(HttpHeaders.UserAgent, MOBILE_USER_AGENT)
            }

            NetworkClientProfile.Getchu -> {
                headers.append(HttpHeaders.UserAgent, DESKTOP_USER_AGENT)
                headers.append(HttpHeaders.Referrer, GETCHU_BASE_URL)
                headers.append(HttpHeaders.Cookie, GETCHU_COOKIE)
                headers.append(
                    HttpHeaders.Accept,
                    "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                )
                headers.append(HttpHeaders.AcceptLanguage, "ja,en-US;q=0.9,en;q=0.8")
                headers.append(HttpHeaders.CacheControl, "no-cache")
            }

            NetworkClientProfile.GitHub -> {
                headers.append(HttpHeaders.UserAgent, MOBILE_USER_AGENT)
                val token = runCatching { AppBuildInfoProvider.current.githubToken }
                    .getOrDefault("")
                headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    if (profile == NetworkClientProfile.Site && installPortableSiteCookies) {
        install(MainSiteCookiePlugin)
    }
}

private object SanitizedNetworkLogger : Logger {
    override fun log(message: String) {
        Logger.DEFAULT.log(message.replace(URL_WITH_QUERY, "$1?<redacted>"))
    }
}

private val URL_WITH_QUERY = Regex("(https?://[^\\s?]+)\\?[^\\s]*")

private val SENSITIVE_HEADERS = setOf(
    HttpHeaders.Authorization,
    HttpHeaders.Cookie,
    HttpHeaders.SetCookie,
    "X-CSRF-TOKEN",
)

private const val MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36"
private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36"
private const val GETCHU_BASE_URL = "https://www.getchu.com/"
private const val GETCHU_COOKIE = "getchu_adalt_flag=getchu.com; gc=gc"
