package com.yenaly.han1meviewer.ui.navigation.main

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.yenaly.han1meviewer.ui.navigation.settings.DownloadSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.HKeyframesRoute
import com.yenaly.han1meviewer.ui.navigation.settings.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.MpvPlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.PlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.settings.SharedHKeyframesRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

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

/**
 * @param sourceJson 待裁剪图片的 `PlatformFile`，序列化成字符串走路由参数。
 *   `PlatformFile` 在 commonMain 是 `@Serializable`（`PlatformFileSerializer`），
 *   这里照搬 [SearchRoute.advancedSearchJson] 的做法——比塞进 NavHost 层的
 *   `remember` 更抗配置变更。
 */
@Serializable
data class AvatarCropRoute(
    val sourceJson: String,
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

/* 原来是三个独立 Activity（LoginActivity / ManualInputCookiesActivity /
 * CloudflareActivity），Step 17 合并进导航图。 */

@Serializable
data object LoginRoute

@Serializable
data object ManualCookiesRoute

@Serializable
data class CloudflareRoute(val url: String)

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
    }
}
