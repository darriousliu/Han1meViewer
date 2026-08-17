package com.yenaly.han1meviewer.ui.navigation.main

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.ui.navigation.AccountRoute
import com.yenaly.han1meviewer.ui.navigation.AvatarCropRoute
import com.yenaly.han1meviewer.ui.navigation.CloudflareRoute
import com.yenaly.han1meviewer.ui.navigation.CreatorCenterRoute
import com.yenaly.han1meviewer.ui.navigation.DailyCheckInRoute
import com.yenaly.han1meviewer.ui.navigation.DownloadRoute
import com.yenaly.han1meviewer.ui.navigation.DownloadSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.GetchuPreviewDetailRoute
import com.yenaly.han1meviewer.ui.navigation.GetchuPreviewRoute
import com.yenaly.han1meviewer.ui.navigation.HKeyframeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.HKeyframesRoute
import com.yenaly.han1meviewer.ui.navigation.HomeRoute
import com.yenaly.han1meviewer.ui.navigation.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.LoginRoute
import com.yenaly.han1meviewer.ui.navigation.ManualCookiesRoute
import com.yenaly.han1meviewer.ui.navigation.MpvPlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.MyFavVideoRoute
import com.yenaly.han1meviewer.ui.navigation.MyPlaylistRoute
import com.yenaly.han1meviewer.ui.navigation.MyWatchLaterRoute
import com.yenaly.han1meviewer.ui.navigation.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.PlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.PreviewCommentRoute
import com.yenaly.han1meviewer.ui.navigation.PreviewRoute
import com.yenaly.han1meviewer.ui.navigation.SearchRoute
import com.yenaly.han1meviewer.ui.navigation.SharedHKeyframesRoute
import com.yenaly.han1meviewer.ui.navigation.SubscriptionRoute
import com.yenaly.han1meviewer.ui.navigation.VideoRoute
import com.yenaly.han1meviewer.ui.navigation.WatchHistoryRoute
import kotlin.reflect.KClass

enum class MainDestinationSpec(
    val drawerDestination: MainDrawerDestination?,
    val routeClass: KClass<*>,
    val drawerEnabled: Boolean,
) {
    Home(
        drawerDestination = MainDrawerDestination.Home,
        routeClass = HomeRoute::class,
        drawerEnabled = true,
    ),
    WatchHistory(
        drawerDestination = MainDrawerDestination.WatchHistory,
        routeClass = WatchHistoryRoute::class,
        drawerEnabled = false,
    ),
    MyFavVideo(
        drawerDestination = MainDrawerDestination.FavVideo,
        routeClass = MyFavVideoRoute::class,
        drawerEnabled = false,
    ),
    MyWatchLater(
        drawerDestination = MainDrawerDestination.WatchLater,
        routeClass = MyWatchLaterRoute::class,
        drawerEnabled = false,
    ),
    MyPlaylist(
        drawerDestination = MainDrawerDestination.Playlist,
        routeClass = MyPlaylistRoute::class,
        drawerEnabled = false,
    ),
    Subscription(
        drawerDestination = MainDrawerDestination.Subscription,
        routeClass = SubscriptionRoute::class,
        drawerEnabled = false,
    ),
    DailyCheckIn(
        drawerDestination = MainDrawerDestination.DailyCheckIn,
        routeClass = DailyCheckInRoute::class,
        drawerEnabled = false,
    ),
    Download(
        drawerDestination = MainDrawerDestination.Download,
        routeClass = DownloadRoute::class,
        drawerEnabled = false,
    ),
    CreatorCenter(
        drawerDestination = MainDrawerDestination.CreatorCenter,
        routeClass = CreatorCenterRoute::class,
        drawerEnabled = false,
    ),
    Account(
        drawerDestination = null,
        routeClass = AccountRoute::class,
        drawerEnabled = false,
    ),
    AvatarCrop(
        drawerDestination = null,
        routeClass = AvatarCropRoute::class,
        drawerEnabled = false,
    ),
    SettingsHome(
        drawerDestination = MainDrawerDestination.Settings,
        routeClass = HomeSettingsRoute::class,
        drawerEnabled = false,
    ),
    SettingsPlayer(
        drawerDestination = MainDrawerDestination.Settings,
        routeClass = PlayerSettingsRoute::class,
        drawerEnabled = false,
    ),
    SettingsNetwork(
        drawerDestination = MainDrawerDestination.Settings,
        routeClass = NetworkSettingsRoute::class,
        drawerEnabled = false,
    ),
    SettingsDownload(
        drawerDestination = MainDrawerDestination.Settings,
        routeClass = DownloadSettingsRoute::class,
        drawerEnabled = false,
    ),
    SettingsMpv(
        drawerDestination = MainDrawerDestination.Settings,
        routeClass = MpvPlayerSettingsRoute::class,
        drawerEnabled = false,
    ),
    SettingsHKeyframes(
        drawerDestination = MainDrawerDestination.Settings,
        routeClass = HKeyframesRoute::class,
        drawerEnabled = false,
    ),
    SettingsSharedHKeyframes(
        drawerDestination = MainDrawerDestination.Settings,
        routeClass = SharedHKeyframesRoute::class,
        drawerEnabled = false,
    ),
    SettingsHKeyframeSettings(
        drawerDestination = MainDrawerDestination.Settings,
        routeClass = HKeyframeSettingsRoute::class,
        drawerEnabled = false,
    ),
    Search(
        drawerDestination = null,
        routeClass = SearchRoute::class,
        drawerEnabled = false,
    ),
    Preview(
        drawerDestination = null,
        routeClass = PreviewRoute::class,
        drawerEnabled = false,
    ),
    GetchuPreview(
        drawerDestination = null,
        routeClass = GetchuPreviewRoute::class,
        drawerEnabled = false,
    ),
    GetchuPreviewDetail(
        drawerDestination = null,
        routeClass = GetchuPreviewDetailRoute::class,
        drawerEnabled = false,
    ),
    PreviewComment(
        drawerDestination = null,
        routeClass = PreviewCommentRoute::class,
        drawerEnabled = false,
    ),
    Video(
        drawerDestination = null,
        routeClass = VideoRoute::class,
        drawerEnabled = false,
    ),
    Login(
        drawerDestination = null,
        routeClass = LoginRoute::class,
        drawerEnabled = false,
    ),
    ManualCookies(
        drawerDestination = null,
        routeClass = ManualCookiesRoute::class,
        drawerEnabled = false,
    ),
    Cloudflare(
        drawerDestination = null,
        routeClass = CloudflareRoute::class,
        drawerEnabled = false,
    );

    companion object {
        fun fromDestination(destination: NavDestination?): MainDestinationSpec? {
            if (destination == null) return null
            return entries.firstOrNull { destination.hasRoute(it.routeClass) }
        }

        /** nav3 版：返回栈里直接就是路由实例，不再有 `NavDestination` 这层 */
        fun fromKey(key: NavKey?): MainDestinationSpec? {
            if (key == null) return null
            return entries.firstOrNull { it.routeClass.isInstance(key) }
        }
    }
}
