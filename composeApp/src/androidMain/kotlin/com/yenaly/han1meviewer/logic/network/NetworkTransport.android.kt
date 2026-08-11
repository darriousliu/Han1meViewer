package com.yenaly.han1meviewer.logic.network

import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import java.io.File
import java.nio.charset.Charset

internal fun File.asNetworkUpload(contentType: String): NetworkUpload = NetworkUpload(
    filename = name,
    contentType = contentType,
    contentLength = length(),
    openChannel = { inputStream().toByteReadChannel() },
)

internal fun RawNetworkResponse.successBodyString(): String? =
    successBody?.toString(responseCharset())

internal fun RawNetworkResponse.errorBodyString(): String? =
    errorBody?.toString(responseCharset())

private fun RawNetworkResponse.responseCharset(): Charset = charsetName
    ?.let { name -> runCatching { Charset.forName(name) }.getOrNull() }
    ?: Charsets.UTF_8
