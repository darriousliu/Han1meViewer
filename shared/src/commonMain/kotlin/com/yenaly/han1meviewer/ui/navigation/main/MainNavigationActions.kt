package com.yenaly.han1meviewer.ui.navigation.main

import androidx.navigation3.runtime.NavBackStack
import com.yenaly.han1meviewer.ui.navigation.CreatorCenterRoute
import com.yenaly.han1meviewer.ui.navigation.DailyCheckInRoute
import com.yenaly.han1meviewer.ui.navigation.DownloadRoute
import com.yenaly.han1meviewer.ui.navigation.HanimeRoute
import com.yenaly.han1meviewer.ui.navigation.HomeRoute
import com.yenaly.han1meviewer.ui.navigation.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.MyFavVideoRoute
import com.yenaly.han1meviewer.ui.navigation.MyPlaylistRoute
import com.yenaly.han1meviewer.ui.navigation.MyWatchLaterRoute
import com.yenaly.han1meviewer.ui.navigation.SubscriptionRoute
import com.yenaly.han1meviewer.ui.navigation.WatchHistoryRoute
import com.yenaly.han1meviewer.ui.navigation.navigateSafely

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
