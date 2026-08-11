package com.yenaly.han1meviewer.util

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.text.HexFormat

/** Encodes without line wrapping, matching Android's previous `Base64.NO_WRAP` output. */
@OptIn(ExperimentalEncodingApi::class)
internal fun ByteArray.encodeBase64(): String = Base64.Default.encode(this)

/**
 * Decodes legacy Android `Base64.DEFAULT` values.
 *
 * Kotlin's Base64 implementation is deliberately fed a normalized value so exports that omit
 * padding or contain ASCII whitespace continue to import exactly as before.
 */
@OptIn(ExperimentalEncodingApi::class)
internal fun String.decodeBase64ToString(): String = Base64.Default
    .decode(normalizeLegacyBase64())
    .decodeToString()

/** Produces the same two-character, upper-case representation as the previous `%02X` loop. */
@OptIn(ExperimentalStdlibApi::class)
internal fun ByteArray.toUpperHexString(): String = toHexString(HexFormat.UpperCase)

private fun String.normalizeLegacyBase64(): String {
    val compact = filterNot(Char::isAsciiWhitespace)
    return when (compact.length % BASE64_BLOCK_SIZE) {
        0 -> compact
        2 -> "$compact=="
        3 -> "$compact="
        else -> compact // Keep malformed input malformed so the decoder reports it to the caller.
    }
}

private fun Char.isAsciiWhitespace(): Boolean = this == ' ' || this in '\t'..'\r'

private const val BASE64_BLOCK_SIZE = 4
