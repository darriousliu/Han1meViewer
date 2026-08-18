package io.github.darriousliu.han1meviewer.core.model

interface VideoItemType {
    val title: String
    val coverUrl: String
    val videoCode: String
    val duration: String?
    val views: String?
    val reviews: String?
    val currentArtist: String?
    val uploadTime: String?
}