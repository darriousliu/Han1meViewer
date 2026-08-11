package com.yenaly.han1meviewer.playback.model

import com.yenaly.han1meviewer.ResolutionLinkMap

fun ResolutionLinkMap.toPlaybackSource(
    id: String,
    title: String,
    coverUrl: String? = null,
    preferredQualityId: String? = null,
    headers: Map<String, String> = emptyMap(),
): PlaybackSource = PlaybackSource(
    id = id,
    title = title,
    posterUri = coverUrl,
    preferredQualityId = preferredQualityId,
    headers = headers,
    qualities = entries.map { (quality, link) ->
        QualityVariant(
            id = quality,
            label = quality,
            uri = link.link,
            mimeType = link.subtype?.let { "video/$it" },
        )
    },
)
