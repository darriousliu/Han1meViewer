package io.github.darriousliu.han1meviewer.ui.navigation.main

import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.baseline_creator_center_24
import io.github.darriousliu.han1meviewer.core.resource.creator_center
import io.github.darriousliu.han1meviewer.core.resource.download
import io.github.darriousliu.han1meviewer.core.resource.fav_video
import io.github.darriousliu.han1meviewer.core.resource.has_mastur
import io.github.darriousliu.han1meviewer.core.resource.home_page
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_download_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_favorite_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_history_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_home_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_list_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_settings_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_thumb_up_alt_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_watch_later_24
import io.github.darriousliu.han1meviewer.core.resource.ic_subscribtion
import io.github.darriousliu.han1meviewer.core.resource.my_subscribe
import io.github.darriousliu.han1meviewer.core.resource.play_list
import io.github.darriousliu.han1meviewer.core.resource.settings
import io.github.darriousliu.han1meviewer.core.resource.watch_history
import io.github.darriousliu.han1meviewer.core.resource.watch_later
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
