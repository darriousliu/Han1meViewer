package com.yenaly.han1meviewer.logic.network

import io.ktor.utils.io.ByteReadChannel

/** A fully consumed HTTP response used by the HTML/form APIs. */
data class RawNetworkResponse(
    val statusCode: Int,
    val reason: String,
    val headers: Map<String, List<String>>,
    val successBody: ByteArray?,
    val errorBody: ByteArray?,
    val charsetName: String?,
) {
    val isSuccessful: Boolean get() = statusCode in 200..299

    fun headerValues(name: String): List<String> = headers.entries
        .firstOrNull { (headerName) -> headerName.equals(name, ignoreCase = true) }
        ?.value
        .orEmpty()
}

/** A response whose successful body remains attached to the underlying HTTP stream. */
data class StreamingNetworkResponse(
    val statusCode: Int,
    val reason: String,
    val headers: Map<String, List<String>>,
    val successBody: ByteReadChannel?,
    val errorBody: ByteArray?,
    val charsetName: String?,
) {
    val isSuccessful: Boolean get() = statusCode in 200..299

    fun headerValues(name: String): List<String> = headers.entries
        .firstOrNull { (headerName) -> headerName.equals(name, ignoreCase = true) }
        ?.value
        .orEmpty()
}

/** A platform-neutral multipart upload. [openChannel] must return a fresh channel for each call. */
class NetworkUpload(
    val filename: String,
    val contentType: String,
    val contentLength: Long?,
    val openChannel: () -> ByteReadChannel,
)

/** Raised by typed HTTP APIs when the server returns a non-success status. */
class NetworkStatusException(
    val response: RawNetworkResponse,
) : Exception("HTTP ${response.statusCode} ${response.reason}".trimEnd())
