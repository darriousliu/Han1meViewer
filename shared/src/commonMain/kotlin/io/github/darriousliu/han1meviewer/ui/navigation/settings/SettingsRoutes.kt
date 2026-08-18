package io.github.darriousliu.han1meviewer.ui.navigation.settings

import androidx.navigation3.runtime.NavKey
import io.github.darriousliu.han1meviewer.ui.navigation.DownloadSettingsRoute
import io.github.darriousliu.han1meviewer.ui.navigation.HKeyframeSettingsRoute
import io.github.darriousliu.han1meviewer.ui.navigation.HKeyframesRoute
import io.github.darriousliu.han1meviewer.ui.navigation.HomeSettingsRoute
import io.github.darriousliu.han1meviewer.ui.navigation.MpvPlayerSettingsRoute
import io.github.darriousliu.han1meviewer.ui.navigation.NetworkSettingsRoute
import io.github.darriousliu.han1meviewer.ui.navigation.PlayerSettingsRoute
import io.github.darriousliu.han1meviewer.ui.navigation.SharedHKeyframesRoute
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.download_settings
import io.github.darriousliu.han1meviewer.core.resource.h_keyframe_manage
import io.github.darriousliu.han1meviewer.core.resource.h_keyframe_settings
import io.github.darriousliu.han1meviewer.core.resource.mpv_advanced_settings
import io.github.darriousliu.han1meviewer.core.resource.network_settings
import io.github.darriousliu.han1meviewer.core.resource.player_settings
import io.github.darriousliu.han1meviewer.core.resource.settings
import io.github.darriousliu.han1meviewer.core.resource.shared_h_keyframe_manage
import kotlin.reflect.KClass
import org.jetbrains.compose.resources.StringResource

enum class SettingsDestinationSpec(
    val titleRes: StringResource,
    val screenClassName: String,
    val routeClass: KClass<*>,
    val showToolbar: Boolean = true,
) {
    Home(
        titleRes = Res.string.settings,
        screenClassName = "HomeSettingsScreen",
        routeClass = HomeSettingsRoute::class,
    ),
    Player(
        titleRes = Res.string.player_settings,
        screenClassName = "PlayerSettingsScreen",
        routeClass = PlayerSettingsRoute::class,
    ),
    Network(
        titleRes = Res.string.network_settings,
        screenClassName = "NetworkSettingsScreen",
        routeClass = NetworkSettingsRoute::class,
    ),
    Download(
        titleRes = Res.string.download_settings,
        screenClassName = "DownloadSettingsScreen",
        routeClass = DownloadSettingsRoute::class,
    ),
    Mpv(
        titleRes = Res.string.mpv_advanced_settings,
        screenClassName = "MpvPlayerSettingsScreen",
        routeClass = MpvPlayerSettingsRoute::class,
    ),
    HKeyframes(
        titleRes = Res.string.h_keyframe_manage,
        screenClassName = "HKeyframesScreen",
        routeClass = HKeyframesRoute::class,
    ),
    SharedHKeyframes(
        titleRes = Res.string.shared_h_keyframe_manage,
        screenClassName = "SharedHKeyframesScreen",
        routeClass = SharedHKeyframesRoute::class,
    ),
    HKeyframeSettings(
        titleRes = Res.string.h_keyframe_settings,
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
