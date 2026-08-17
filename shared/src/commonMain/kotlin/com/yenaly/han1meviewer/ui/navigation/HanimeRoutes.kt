package com.yenaly.han1meviewer.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/*
 * 全 App 的导航目的地。Step 19 从 androidMain 的 `ui/navigation/main/MainRoutes.kt`
 * 与 `ui/navigation/settings/SettingsRoutes.kt` 收进来，统一放在 commonMain 的
 * 同一个包里——两个原因：
 *
 * 1. nav3 的 `NavKey` 与路由类都是纯数据，本来就没有平台依赖
 * 2. **sealed 层级要求所有直接实现同包同模块**。收在一起之后
 *    `rememberNavBackStack` 的持久化（进程死亡后恢复返回栈）由 kotlinx 的
 *    sealed 自动多态兜住，不需要手写 `SerializersModule` 逐个注册——
 *    那种注册漏了只有运行时才炸。
 *
 * 两个 `*DestinationSpec` 枚举留在原来的包里，反向 import 这里即可。
 */

/** 所有导航目的地的公共父类型。新增路由**必须**实现它，否则进不了返回栈。 */
@Serializable
sealed interface HanimeRoute : NavKey

// ---- 抽屉十项 ----

@Serializable
data object HomeRoute : HanimeRoute

@Serializable
data object WatchHistoryRoute : HanimeRoute

@Serializable
data object MyFavVideoRoute : HanimeRoute

@Serializable
data object MyWatchLaterRoute : HanimeRoute

@Serializable
data object MyPlaylistRoute : HanimeRoute

@Serializable
data object SubscriptionRoute : HanimeRoute

@Serializable
data object DailyCheckInRoute : HanimeRoute

@Serializable
data object DownloadRoute : HanimeRoute

@Serializable
data object CreatorCenterRoute : HanimeRoute

// ---- 账号 ----

@Serializable
data object AccountRoute : HanimeRoute

/**
 * @param sourceJson 待裁剪图片的 `PlatformFile`，序列化成字符串走路由参数。
 *   `PlatformFile` 在 commonMain 是 `@Serializable`（`PlatformFileSerializer`），
 *   这里照搬 [SearchRoute.advancedSearchJson] 的做法——比塞进 NavHost 层的
 *   `remember` 更抗配置变更。
 */
@Serializable
data class AvatarCropRoute(
    val sourceJson: String,
) : HanimeRoute

// ---- 搜索 / 预览 / 视频 ----

@Serializable
data class SearchRoute(
    val query: String? = null,
    val advancedSearchJson: String? = null,
) : HanimeRoute

@Serializable
data object PreviewRoute : HanimeRoute

@Serializable
data object GetchuPreviewRoute : HanimeRoute

@Serializable
data class GetchuPreviewDetailRoute(
    val id: String,
) : HanimeRoute

@Serializable
data class PreviewCommentRoute(
    val date: String,
    val dateCode: String,
) : HanimeRoute

@Serializable
data class VideoRoute(
    val videoCode: String,
    val localUri: String? = null,
) : HanimeRoute

// ---- 原来是三个独立 Activity（LoginActivity / ManualInputCookiesActivity /
//      CloudflareActivity），Step 17 合并进导航图 ----

@Serializable
data object LoginRoute : HanimeRoute

@Serializable
data object ManualCookiesRoute : HanimeRoute

@Serializable
data class CloudflareRoute(val url: String) : HanimeRoute

// ---- 设置八页 ----

@Serializable
data object HomeSettingsRoute : HanimeRoute

@Serializable
data object PlayerSettingsRoute : HanimeRoute

@Serializable
data object NetworkSettingsRoute : HanimeRoute

@Serializable
data object DownloadSettingsRoute : HanimeRoute

@Serializable
data object MpvPlayerSettingsRoute : HanimeRoute

@Serializable
data object HKeyframesRoute : HanimeRoute

@Serializable
data object SharedHKeyframesRoute : HanimeRoute

@Serializable
data object HKeyframeSettingsRoute : HanimeRoute
