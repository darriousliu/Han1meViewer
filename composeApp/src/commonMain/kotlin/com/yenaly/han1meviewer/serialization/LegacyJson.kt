@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.yenaly.han1meviewer.serialization

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Compatibility codec for response fields previously read through Android's lenient JSON parser. */
internal val LegacyJson = Json {
    isLenient = true
    allowComments = true
}

internal fun String.parseLegacyJsonObject(): JsonObject =
    LegacyJson.parseToJsonElement(normalizeAndroidJsonExtensions()) as? JsonObject
        ?: throw SerializationException("Expected a JSON object")

internal fun String.parseLegacyJsonObjectOrNull(): JsonObject? =
    runCatching { parseLegacyJsonObject() }.getOrNull()

internal fun String.requiredLegacyJsonValue(key: String): String =
    parseLegacyJsonObject()[key]?.asLegacyJsonString()
        ?: throw SerializationException("No value for $key")

internal fun JsonElement?.asLegacyBooleanOrFalse(): Boolean {
    val primitive = this as? JsonPrimitive ?: return false
    if (primitive === JsonNull) return false
    return primitive.legacyPrimitiveContent().equals("true", ignoreCase = true)
}

internal fun JsonElement?.asLegacyOptionalString(): String = when (this) {
    null -> ""
    else -> asLegacyJsonString()
}

internal fun JsonElement.asLegacyJsonString(): String = when (this) {
    JsonNull -> "null"
    is JsonPrimitive -> legacyPrimitiveContent().let { value ->
        when {
            isString -> value
            value == "true" || value == "false" -> value
            value.toLongOrNull() != null -> value.toLong().toString()
            value.toDoubleOrNull() != null -> value.toDouble().toString()
            else -> value
        }
    }

    else -> toString()
}

private fun JsonPrimitive.legacyPrimitiveContent(): String = content

/** Normalizes the two Android JSONTokener extensions not handled by kotlinx.serialization. */
private fun String.normalizeAndroidJsonExtensions(): String {
    val normalized = StringBuilder(length)
    var index = 0
    var lastSignificantCharacter: Char? = null
    while (index < length) {
        when (this[index]) {
            '"' -> {
                val start = index++
                while (index < length) {
                    when (this[index++]) {
                        '\\' -> if (index < length) index++
                        '"' -> break
                    }
                }
                normalized.append(this, start, index)
                lastSignificantCharacter = '"'
            }

            '\'' -> {
                if (lastSignificantCharacter.isAndroidQuotedTokenPrefix()) {
                    val (value, nextIndex) = readAndroidSingleQuotedString(index)
                    normalized.append(JsonPrimitive(value))
                    index = nextIndex
                    lastSignificantCharacter = '\''
                } else {
                    normalized.append(this[index++])
                    lastSignificantCharacter = '\''
                }
            }

            '#' -> {
                while (index < length && this[index] != '\n' && this[index] != '\r') index++
            }

            '/' -> {
                val commentEnd = when (getOrNull(index + 1)) {
                    '/' -> indexOfLineCommentEnd(index + 2)
                    '*' -> indexOfBlockCommentEnd(index + 2)
                    else -> null
                }
                if (commentEnd != null) {
                    normalized.append(this, index, commentEnd)
                    index = commentEnd
                } else {
                    normalized.append(this[index++])
                    lastSignificantCharacter = '/'
                }
            }

            else -> {
                val character = this[index++]
                normalized.append(character)
                if (!character.isWhitespace()) lastSignificantCharacter = character
            }
        }
    }
    return normalized.toString()
}

private fun Char?.isAndroidQuotedTokenPrefix(): Boolean =
    this == null || this == '{' || this == '[' || this == ',' || this == ':' ||
        this == '=' || this == ';' || this == '>'

private fun String.indexOfLineCommentEnd(startIndex: Int): Int {
    var index = startIndex
    while (index < length && this[index] != '\n' && this[index] != '\r') index++
    return index
}

private fun String.indexOfBlockCommentEnd(startIndex: Int): Int {
    var index = startIndex
    while (index + 1 < length && !(this[index] == '*' && this[index + 1] == '/')) index++
    return if (index + 1 < length) index + 2 else length
}

private fun String.readAndroidSingleQuotedString(startIndex: Int): Pair<String, Int> {
    val value = StringBuilder()
    var index = startIndex + 1
    while (index < length) {
        val character = this[index++]
        when (character) {
            '\'' -> return value.toString() to index
            '\\' -> {
                if (index >= length) throw SerializationException("Unterminated escape sequence")
                when (val escaped = this[index++]) {
                    'b' -> value.append('\b')
                    'f' -> value.append('\u000C')
                    'n' -> value.append('\n')
                    'r' -> value.append('\r')
                    't' -> value.append('\t')
                    'u' -> {
                        if (index + 4 > length) {
                            throw SerializationException("Unterminated unicode escape sequence")
                        }
                        val codePoint = substring(index, index + 4).toIntOrNull(16)
                            ?: throw SerializationException("Invalid unicode escape sequence")
                        value.append(codePoint.toChar())
                        index += 4
                    }

                    else -> value.append(escaped)
                }
            }

            else -> value.append(character)
        }
    }
    throw SerializationException("Unterminated single-quoted string")
}
