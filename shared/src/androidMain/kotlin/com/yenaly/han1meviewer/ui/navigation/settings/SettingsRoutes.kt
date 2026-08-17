package com.yenaly.han1meviewer.ui.navigation.settings

import androidx.navigation3.runtime.NavKey
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.navigation.DownloadSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.HKeyframeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.HKeyframesRoute
import com.yenaly.han1meviewer.ui.navigation.HomeSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.MpvPlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.NetworkSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.PlayerSettingsRoute
import com.yenaly.han1meviewer.ui.navigation.SharedHKeyframesRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

enum class SettingsDestinationSpec(
    val titleRes: Int,
    val screenClassName: String,
    val routeClass: KClass<*>,
    val showToolbar: Boolean = true,
) {
    Home(
        titleRes = R.string.settings,
        screenClassName = "HomeSettingsScreen",
        routeClass = HomeSettingsRoute::class,
    ),
    Player(
        titleRes = R.string.player_settings,
        screenClassName = "PlayerSettingsScreen",
        routeClass = PlayerSettingsRoute::class,
    ),
    Network(
        titleRes = R.string.network_settings,
        screenClassName = "NetworkSettingsScreen",
        routeClass = NetworkSettingsRoute::class,
    ),
    Download(
        titleRes = R.string.download_settings,
        screenClassName = "DownloadSettingsScreen",
        routeClass = DownloadSettingsRoute::class,
    ),
    Mpv(
        titleRes = R.string.mpv_advanced_settings,
        screenClassName = "MpvPlayerSettingsScreen",
        routeClass = MpvPlayerSettingsRoute::class,
    ),
    HKeyframes(
        titleRes = R.string.h_keyframe_manage,
        screenClassName = "HKeyframesScreen",
        routeClass = HKeyframesRoute::class,
    ),
    SharedHKeyframes(
        titleRes = R.string.shared_h_keyframe_manage,
        screenClassName = "SharedHKeyframesScreen",
        routeClass = SharedHKeyframesRoute::class,
    ),
    HKeyframeSettings(
        titleRes = R.string.h_keyframe_settings,
        screenClassName = "HKeyframeSettingsScreen",
        routeClass = HKeyframeSettingsRoute::class,
    );

    companion object {
        /** nav3 版：返回栈里直接就是路由实例，不再有 `NavDestination` 这层 */
        fun fromKey(key: NavKey?): SettingsDestinationSpec? {
            if (key == null) return null
            return entries.firstOrNull { it.routeClass.isInstance(key) }
        }
    }
}
