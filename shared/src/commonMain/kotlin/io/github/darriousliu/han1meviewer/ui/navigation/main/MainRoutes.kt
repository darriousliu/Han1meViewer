package io.github.darriousliu.han1meviewer.ui.navigation.main

import androidx.navigation3.runtime.NavKey
import io.github.darriousliu.han1meviewer.core.navigation.AccountRoute
import io.github.darriousliu.han1meviewer.core.navigation.AvatarCropRoute
import io.github.darriousliu.han1meviewer.core.navigation.CloudflareRoute
import io.github.darriousliu.han1meviewer.core.navigation.CreatorCenterRoute
import io.github.darriousliu.han1meviewer.core.navigation.DailyCheckInRoute
import io.github.darriousliu.han1meviewer.core.navigation.DownloadRoute
import io.github.darriousliu.han1meviewer.core.navigation.DownloadSettingsRoute
import io.github.darriousliu.han1meviewer.core.navigation.GetchuPreviewDetailRoute
import io.github.darriousliu.han1meviewer.core.navigation.GetchuPreviewRoute
import io.github.darriousliu.han1meviewer.core.navigation.HKeyframeSettingsRoute
import io.github.darriousliu.han1meviewer.core.navigation.HKeyframesRoute
import io.github.darriousliu.han1meviewer.core.navigation.HomeRoute
import io.github.darriousliu.han1meviewer.core.navigation.HomeSettingsRoute
import io.github.darriousliu.han1meviewer.core.navigation.LoginRoute
import io.github.darriousliu.han1meviewer.core.navigation.ManualCookiesRoute
import io.github.darriousliu.han1meviewer.core.navigation.MpvPlayerSettingsRoute
import io.github.darriousliu.han1meviewer.core.navigation.MyFavVideoRoute
import io.github.darriousliu.han1meviewer.core.navigation.MyPlaylistRoute
import io.github.darriousliu.han1meviewer.core.navigation.MyWatchLaterRoute
import io.github.darriousliu.han1meviewer.core.navigation.NetworkSettingsRoute
import io.github.darriousliu.han1meviewer.core.navigation.PlayerSettingsRoute
import io.github.darriousliu.han1meviewer.core.navigation.PreviewCommentRoute
import io.github.darriousliu.han1meviewer.core.navigation.PreviewRoute
import io.github.darriousliu.han1meviewer.core.navigation.SearchRoute
import io.github.darriousliu.han1meviewer.core.navigation.SharedHKeyframesRoute
import io.github.darriousliu.han1meviewer.core.navigation.SubscriptionRoute
import io.github.darriousliu.han1meviewer.core.navigation.VideoRoute
import io.github.darriousliu.han1meviewer.core.navigation.WatchHistoryRoute
import kotlin.reflect.KClass
import io.github.darriousliu.han1meviewer.core.model.GetchuPreviewDetail
import io.github.darriousliu.han1meviewer.core.model.GetchuPreview

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
        /** nav3 版：返回栈里直接就是路由实例，不再有 `NavDestination` 这层 */
        fun fromKey(key: NavKey?): MainDestinationSpec? {
            if (key == null) return null
            return entries.firstOrNull { it.routeClass.isInstance(key) }
        }
    }
}
