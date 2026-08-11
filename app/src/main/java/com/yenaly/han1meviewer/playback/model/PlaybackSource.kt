package com.yenaly.han1meviewer.playback.model

/** A video and all of its directly selectable quality variants. */
data class PlaybackSource(
    val id: String,
    val title: String,
    val qualities: List<QualityVariant>,
    val preferredQualityId: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val posterUri: String? = null,
) {
    init {
        require(id.isNotBlank()) { "Playback source id cannot be blank." }
        require(qualities.isNotEmpty()) { "Playback source must contain at least one quality." }
        require(qualities.map(QualityVariant::id).distinct().size == qualities.size) {
            "Quality ids must be unique within a playback source."
        }
    }

    fun resolveQuality(requestedQualityId: String? = null): QualityVariant {
        val targetId = requestedQualityId ?: preferredQualityId
        return qualities.firstOrNull { it.id == targetId } ?: qualities.first()
    }

    fun headersFor(quality: QualityVariant): Map<String, String> = headers + quality.headers
}
