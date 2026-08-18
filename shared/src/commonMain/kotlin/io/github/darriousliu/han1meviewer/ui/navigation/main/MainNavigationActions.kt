package io.github.darriousliu.han1meviewer.ui.navigation.main

import androidx.navigation3.runtime.NavBackStack
import io.github.darriousliu.han1meviewer.ui.navigation.CreatorCenterRoute
import io.github.darriousliu.han1meviewer.ui.navigation.DailyCheckInRoute
import io.github.darriousliu.han1meviewer.ui.navigation.DownloadRoute
import io.github.darriousliu.han1meviewer.ui.navigation.HanimeRoute
import io.github.darriousliu.han1meviewer.ui.navigation.HomeRoute
import io.github.darriousliu.han1meviewer.ui.navigation.HomeSettingsRoute
import io.github.darriousliu.han1meviewer.ui.navigation.MyFavVideoRoute
import io.github.darriousliu.han1meviewer.ui.navigation.MyPlaylistRoute
import io.github.darriousliu.han1meviewer.ui.navigation.MyWatchLaterRoute
import io.github.darriousliu.han1meviewer.ui.navigation.SubscriptionRoute
import io.github.darriousliu.han1meviewer.ui.navigation.WatchHistoryRoute
import io.github.darriousliu.han1meviewer.ui.navigation.navigateSafely

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
