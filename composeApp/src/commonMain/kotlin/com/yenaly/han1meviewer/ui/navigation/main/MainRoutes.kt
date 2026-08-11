package com.yenaly.han1meviewer.ui.navigation.main

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object WatchHistoryRoute

@Serializable
object MyFavVideoRoute

@Serializable
object MyWatchLaterRoute

@Serializable
object MyPlaylistRoute

@Serializable
object SubscriptionRoute

@Serializable
object DailyCheckInRoute

@Serializable
object DownloadRoute

@Serializable
object CreatorCenterRoute

@Serializable
object AccountRoute

@Serializable
data class AvatarCropRoute(
    val sourceUri: String,
)

@Serializable
data class SearchRoute(
    val query: String? = null,
    val advancedSearchJson: String? = null,
)

@Serializable
object PreviewRoute

@Serializable
object GetchuPreviewRoute

@Serializable
data class GetchuPreviewDetailRoute(
    val id: String,
)

@Serializable
data class PreviewCommentRoute(
    val date: String,
    val dateCode: String,
)

@Serializable
data class VideoRoute(
    val videoCode: String,
    val localUri: String? = null,
)
