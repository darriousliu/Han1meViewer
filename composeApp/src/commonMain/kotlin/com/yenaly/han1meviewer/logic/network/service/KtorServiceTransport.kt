package com.yenaly.han1meviewer.logic.network.service

import com.yenaly.han1meviewer.logic.network.RawNetworkResponse
import com.yenaly.han1meviewer.logic.network.StreamingNetworkResponse
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom

internal class KtorServiceTransport(
    private val client: HttpClient,
    baseUrl: String,
) {
    private val baseUrl = Url(baseUrl.also {
        require(it.endsWith('/')) { "Base URL must end in /: $it" }
    })

    suspend fun raw(
        method: HttpMethod = HttpMethod.Get,
        pathSegments: List<String> = emptyList(),
        urlOverride: String? = null,
        configure: HttpRequestBuilder.() -> Unit = {},
    ): RawNetworkResponse = execute(method, pathSegments, urlOverride, configure).toRawResponse()

    suspend fun <T> streaming(
        method: HttpMethod = HttpMethod.Get,
        pathSegments: List<String> = emptyList(),
        urlOverride: String? = null,
        configure: HttpRequestBuilder.() -> Unit = {},
        consumer: suspend (StreamingNetworkResponse) -> T,
    ): T = client.prepareRequest {
        configureRequest(method, pathSegments, urlOverride, configure)
    }.execute { response ->
        consumer(response.toStreamingResponse())
    }

    private suspend fun execute(
        method: HttpMethod,
        pathSegments: List<String>,
        urlOverride: String?,
        configure: HttpRequestBuilder.() -> Unit,
    ): HttpResponse = client.request {
        configureRequest(method, pathSegments, urlOverride, configure)
    }

    private fun HttpRequestBuilder.configureRequest(
        method: HttpMethod,
        pathSegments: List<String>,
        urlOverride: String?,
        configure: HttpRequestBuilder.() -> Unit,
    ) {
        this.method = method
        expectSuccess = false
        url.takeFrom(baseUrl)
        if (urlOverride == null) {
            url.appendPathSegments(pathSegments, encodeSlash = true)
        } else {
            // Retrofit's @Url accepts both absolute URLs and paths relative to the service base URL.
            url.takeFrom(urlOverride)
        }
        configure()
    }
}

private suspend fun HttpResponse.toRawResponse(): RawNetworkResponse {
    val responseBody = bodyAsBytes()
    val successful = status.value in 200..299
    return RawNetworkResponse(
        statusCode = status.value,
        reason = status.description,
        headers = headers.toMultiValueMap(),
        successBody = responseBody.takeIf { successful },
        errorBody = responseBody.takeUnless { successful },
        charsetName = headers.charsetName(),
    )
}

private suspend fun HttpResponse.toStreamingResponse(): StreamingNetworkResponse {
    val successful = status.value in 200..299
    return StreamingNetworkResponse(
        statusCode = status.value,
        reason = status.description,
        headers = headers.toMultiValueMap(),
        successBody = if (successful) bodyAsChannel() else null,
        errorBody = if (successful) null else bodyAsBytes(),
        charsetName = headers.charsetName(),
    )
}

private fun io.ktor.http.Headers.toMultiValueMap(): Map<String, List<String>> =
    entries().associate { (name, values) -> name to values.toList() }

private fun io.ktor.http.Headers.charsetName(): String? =
    getAll(HttpHeaders.ContentType)
        ?.asSequence()
        ?.flatMap { value -> value.split(';').asSequence().drop(1) }
        ?.mapNotNull { parameter ->
            val separator = parameter.indexOf('=')
            if (separator < 0) return@mapNotNull null
            val name = parameter.substring(0, separator).trim()
            if (!name.equals("charset", ignoreCase = true)) return@mapNotNull null
            parameter.substring(separator + 1).trim().trim('"').takeIf(String::isNotEmpty)
        }
        ?.firstOrNull()
