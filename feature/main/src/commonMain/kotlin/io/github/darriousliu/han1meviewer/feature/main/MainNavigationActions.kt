package io.github.darriousliu.han1meviewer.feature.main

import androidx.navigation3.runtime.NavBackStack
import io.github.darriousliu.han1meviewer.core.navigation.CreatorCenterRoute
import io.github.darriousliu.han1meviewer.core.navigation.DailyCheckInRoute
import io.github.darriousliu.han1meviewer.core.navigation.DownloadRoute
import io.github.darriousliu.han1meviewer.core.navigation.HanimeRoute
import io.github.darriousliu.han1meviewer.core.navigation.HomeRoute
import io.github.darriousliu.han1meviewer.core.navigation.HomeSettingsRoute
import io.github.darriousliu.han1meviewer.core.navigation.MyFavVideoRoute
import io.github.darriousliu.han1meviewer.core.navigation.MyPlaylistRoute
import io.github.darriousliu.han1meviewer.core.navigation.MyWatchLaterRoute
import io.github.darriousliu.han1meviewer.core.navigation.SubscriptionRoute
import io.github.darriousliu.han1meviewer.core.navigation.WatchHistoryRoute
import io.github.darriousliu.han1meviewer.core.navigation.navigateSafely

private val loginRequiredDrawerItems = setOf(
    MainDrawerDestination.FavVideo,
    MainDrawerDestination.WatchLater,
    MainDrawerDestination.Playlist,
    MainDrawerDestination.Subscription,
)

fun NavBackStack<HanimeRoute>.navigateDrawerDestination(
    destination: MainDrawerDestination,
    isLoggedIn: Boolean,
    onRequireLogin: () -> Unit,
): Boolean {
    if (destination in loginRequiredDrawerItems && !isLoggedIn) {
        onRequireLogin()
        return false
    }

    when (destination) {
        MainDrawerDestination.Home -> navigateSafely(HomeRoute)
        MainDrawerDestination.Settings -> navigateSafely(HomeSettingsRoute)
        MainDrawerDestination.DailyCheckIn -> navigateSafely(DailyCheckInRoute)
        MainDrawerDestination.WatchLater -> navigateSafely(MyWatchLaterRoute)
        MainDrawerDestination.FavVideo -> navigateSafely(MyFavVideoRoute)
        MainDrawerDestination.Playlist -> navigateSafely(MyPlaylistRoute)
        MainDrawerDestination.Subscription -> navigateSafely(SubscriptionRoute)
        MainDrawerDestination.CreatorCenter -> navigateSafely(CreatorCenterRoute)
        MainDrawerDestination.WatchHistory -> navigateSafely(WatchHistoryRoute)
        MainDrawerDestination.Download -> navigateSafely(DownloadRoute)
    }
    return true
}
