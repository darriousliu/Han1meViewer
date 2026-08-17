package com.yenaly.han1meviewer.ui.navigation.main

import han1meviewer.shared.generated.resources.Res
import han1meviewer.shared.generated.resources.baseline_creator_center_24
import han1meviewer.shared.generated.resources.creator_center
import han1meviewer.shared.generated.resources.download
import han1meviewer.shared.generated.resources.fav_video
import han1meviewer.shared.generated.resources.has_mastur
import han1meviewer.shared.generated.resources.home_page
import han1meviewer.shared.generated.resources.ic_baseline_download_24
import han1meviewer.shared.generated.resources.ic_baseline_favorite_24
import han1meviewer.shared.generated.resources.ic_baseline_history_24
import han1meviewer.shared.generated.resources.ic_baseline_home_24
import han1meviewer.shared.generated.resources.ic_baseline_list_24
import han1meviewer.shared.generated.resources.ic_baseline_settings_24
import han1meviewer.shared.generated.resources.ic_baseline_thumb_up_alt_24
import han1meviewer.shared.generated.resources.ic_baseline_watch_later_24
import han1meviewer.shared.generated.resources.ic_subscribtion
import han1meviewer.shared.generated.resources.my_subscribe
import han1meviewer.shared.generated.resources.play_list
import han1meviewer.shared.generated.resources.settings
import han1meviewer.shared.generated.resources.watch_history
import han1meviewer.shared.generated.resources.watch_later
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

enum class MainDrawerDestination(
    val iconRes: DrawableResource,
    val titleRes: StringResource,
    val requiresLogin: Boolean = false,
) {
    Home(
        iconRes = Res.drawable.ic_baseline_home_24,
        titleRes = Res.string.home_page,
    ),
    Settings(
        iconRes = Res.drawable.ic_baseline_settings_24,
        titleRes = Res.string.settings,
    ),
    DailyCheckIn(
        iconRes = Res.drawable.ic_baseline_thumb_up_alt_24,
        titleRes = Res.string.has_mastur,
    ),
    WatchLater(
        iconRes = Res.drawable.ic_baseline_watch_later_24,
        titleRes = Res.string.watch_later,
        requiresLogin = true,
    ),
    FavVideo(
        iconRes = Res.drawable.ic_baseline_favorite_24,
        titleRes = Res.string.fav_video,
        requiresLogin = true,
    ),
    Playlist(
        iconRes = Res.drawable.ic_baseline_list_24,
        titleRes = Res.string.play_list,
        requiresLogin = true,
    ),
    Subscription(
        iconRes = Res.drawable.ic_subscribtion,
        titleRes = Res.string.my_subscribe,
        requiresLogin = true,
    ),
    CreatorCenter(
        iconRes = Res.drawable.baseline_creator_center_24,
        titleRes = Res.string.creator_center,
        requiresLogin = true,
    ),
    WatchHistory(
        iconRes = Res.drawable.ic_baseline_history_24,
        titleRes = Res.string.watch_history,
    ),
    Download(
        iconRes = Res.drawable.ic_baseline_download_24,
        titleRes = Res.string.download,
    ),
}
