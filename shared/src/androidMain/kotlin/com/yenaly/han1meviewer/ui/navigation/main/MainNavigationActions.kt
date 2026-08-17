package com.yenaly.han1meviewer.ui.navigation.main

import android.content.Intent
import androidx.navigation.NavHostController
import com.yenaly.han1meviewer.ui.navigation.navigateSafely
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute

private val loginRequiredDrawerItems = setOf(
    MainDrawerDestination.FavVideo,
    MainDrawerDestination.WatchLater,
    MainDrawerDestination.Playlist,
    MainDrawerDestination.Subscription,
)

fun NavHostController.navigateDrawerDestination(
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

fun NavHostController.handleMainIntent(intent: Intent) {
    if (intent.action == Intent.ACTION_VIEW) {
        val uri = intent.data ?: return
        when (uri.scheme) {
            "http", "https" -> {
                val videoCode = uri.getQueryParameter("v")
                if (videoCode != null) {
                    navigateSafely(VideoRoute(videoCode))
                }
            }

            "file", "content" -> {
                navigateSafely(VideoRoute("-1", uri.toString()))
            }
        }
        return
    }

    // 原来这里还处理 startSearchFromTag / startSearchFromMap / startVideoCode 三个 extra，
    // 全仓没有任何生产者（跨 Activity 时代的遗留），Step 17 合并 Activity 时一并删除。
}
