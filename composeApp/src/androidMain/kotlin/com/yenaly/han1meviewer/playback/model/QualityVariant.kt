package com.yenaly.han1meviewer.playback.model

/**
 * A directly playable representation of one video quality.
 *
 * [uri] intentionally remains a string so HTTP(S), file and content URIs share the same
 * model without leaking an Android or player-specific type into callers.
 */
data class QualityVariant(
    val id: String,
    val label: String = id,
    val uri: String,
    val mimeType: String? = null,
    val headers: Map<String, String> = emptyMap(),
) {
    val isRemote: Boolean
        get() = uri.startsWith("http://", ignoreCase = true) ||
            uri.startsWith("https://", ignoreCase = true)

    val isLocal: Boolean
        get() = !isRemote

    init {
        require(id.isNotBlank()) { "Quality id cannot be blank." }
        require(label.isNotBlank()) { "Quality label cannot be blank." }
        require(uri.isNotBlank()) { "Quality URI cannot be blank." }
    }
}
