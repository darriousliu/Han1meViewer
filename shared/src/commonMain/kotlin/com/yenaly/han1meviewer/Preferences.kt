package com.yenaly.han1meviewer

import com.yenaly.han1meviewer.HanimeConstants.HANIME_URL
import com.yenaly.han1meviewer.Preferences.baseUrl
import com.yenaly.han1meviewer.Preferences.exportSettings
import com.yenaly.han1meviewer.Preferences.loginCookie
import com.yenaly.han1meviewer.mmkv.AccountStore
import com.yenaly.han1meviewer.mmkv.MMKVOwner
import com.yenaly.han1meviewer.mmkv.MMKVProperty
import com.yenaly.han1meviewer.mmkv.MiscStore
import com.yenaly.han1meviewer.mmkv.SettingsStore
import com.yenaly.han1meviewer.mmkv.asMutableStateFlow
import com.yenaly.han1meviewer.mmkv.mmkvBool
import com.yenaly.han1meviewer.mmkv.mmkvFloat
import com.yenaly.han1meviewer.mmkv.mmkvInt
import com.yenaly.han1meviewer.mmkv.mmkvLong
import com.yenaly.han1meviewer.mmkv.mmkvNullableString
import com.yenaly.han1meviewer.mmkv.mmkvString
import com.yenaly.han1meviewer.util.CookieString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * 全局配置。存储后端是 MMKV，key 就是这里的**属性名**。
 *
 * 访问本对象前必须先调用 [com.yenaly.han1meviewer.mmkv.initializeMMKV]——
 * 下面几个 StateFlow 在首次访问时就会读盘。
 *
 * 新增设置项时，如果它该进备份，记得在 [exportSettings] 里补一行。
 */
object Preferences {

    // ---------------- 账号 / 登录态（AccountStore）----------------

    /**
     * 是否登入，一般跟 [loginCookie] 一起赋值。
     */
    val loginStateFlow: MutableStateFlow<Boolean>
            by AccountStore.mmkvBool(false, key = "isAlreadyLogin").asMutableStateFlow()

    var isAlreadyLogin: Boolean
        get() = loginStateFlow.value
        set(value) {
            loginStateFlow.value = value
        }

    /**
     * 保存的 string 格式的登入 cookie。
     */
    val loginCookieStateFlow: MutableStateFlow<CookieString>
            by AccountStore.mmkvCookie(key = "loginCookie").asMutableStateFlow()

    var loginCookie: CookieString
        get() = loginCookieStateFlow.value
        set(value) {
            loginCookieStateFlow.value = value
        }

    val cloudFlareCookieStateFlow: MutableStateFlow<CookieString>
            by AccountStore.mmkvCookie(key = "cloudFlareCookie").asMutableStateFlow()

    var cloudFlareCookie: CookieString
        get() = cloudFlareCookieStateFlow.value
        set(value) {
            cloudFlareCookieStateFlow.value = value
        }

    var updateNodeId: String by AccountStore.mmkvString(EMPTY_STRING)

    var lastUpdatePopupTime: Long by AccountStore.mmkvLong(0L)

    // ---------------- 杂项（MiscStore）----------------

    /** 首页公告上次被关掉的时间。 */
    var lastDismissTime: Long by MiscStore.mmkvLong(0L)

    // ---------------- App / 首页 ----------------

    var usageNoticeAccepted: Boolean by SettingsStore.mmkvBool(false)

    var savedUserId: String by SettingsStore.mmkvString(EMPTY_STRING)

    var appLanguage: String by SettingsStore.mmkvString("system")

    /**
     * 深色模式偏好：`follow_system` / `always_on` / `always_off`。
     *
     * 做成 StateFlow 是为了让 `HanimeTheme` 能直接观察它——原来主题只看
     * `isSystemInDarkTheme()`，靠 Android 的 `AppCompatDelegate.setDefaultNightMode`
     * 改 configuration 间接生效，所以每次切换都得重建 Activity，
     * 而且这个偏好在 desktop/iOS 上完全是死的。
     */
    val useDarkModeStateFlow: MutableStateFlow<String>
            by SettingsStore.mmkvString("always_off", key = "useDarkMode").asMutableStateFlow()

    var useDarkMode: String
        get() = useDarkModeStateFlow.value
        set(value) {
            useDarkModeStateFlow.value = value
        }

    var useDynamicColor: Boolean by SettingsStore.mmkvBool(false)

    var themeColor: String? by SettingsStore.mmkvNullableString()

    var fakeLauncherIcon: String
            by SettingsStore.mmkvString("com.yenaly.han1meviewer.LauncherAliasDefault")

    var tabletMode: Boolean by SettingsStore.mmkvBool(false)

    var disablePredictiveBack: Boolean by SettingsStore.mmkvBool(false)

    var disableMobileDataWarning: Boolean by SettingsStore.mmkvBool(false)

    var disableComments: Boolean by SettingsStore.mmkvBool(false)

    var useLockScreen: Boolean by SettingsStore.mmkvBool(false)

    /**
     * ⚠️ 默认值统一成 `true`。迁移前这个 key 有两个不一致的默认值：
     * `MainActivity.onUserLeaveHint` 读的是 `true`、设置页读的是 `false`，
     * 也就是没动过设置的用户实际能用画中画、但开关显示是关的。
     * 这里取 `true` 保住实际行为，代价是设置页的开关现在会显示成开。
     */
    var allowPipMode: Boolean by SettingsStore.mmkvBool(true)

    var isAnalyticsEnabled: Boolean by SettingsStore.mmkvBool(true)

    /** 逗号分隔，见 `HomeCategoryConfig`。 */
    var homeCategoryOrder: String by SettingsStore.mmkvString(EMPTY_STRING)

    /** 逗号分隔，见 `HomeCategoryConfig`。 */
    var homeCategoryHidden: String by SettingsStore.mmkvString(EMPTY_STRING)

    // ---------------- 更新 ----------------

    var updatePopupIntervalDays: Int by SettingsStore.mmkvInt(0)

    var useCIUpdateChannel: Boolean by SettingsStore.mmkvBool(false)

    /** 是否该弹更新对话框。 */
    @OptIn(ExperimentalTime::class)
    val isUpdateDialogVisible: Boolean
        get() {
            val now = Clock.System.now()
            val lastCheckTime = Instant.fromEpochSeconds(lastUpdatePopupTime)
            return now > lastCheckTime + updatePopupIntervalDays.days
        }

    // ---------------- 播放器 ----------------

    var switchPlayerKernel: String by SettingsStore.mmkvString(PlayerDefaults.PLAYER_KERNEL)

    var showBottomProgress: Boolean by SettingsStore.mmkvBool(true)

    var playerSpeed: Float by SettingsStore.mmkvFloat(PlayerDefaults.SPEED)

    var slideSensitivity: Int by SettingsStore.mmkvInt(PlayerDefaults.PROGRESS_SLIDE_SENSITIVITY)

    var longPressSpeedTime: Float by SettingsStore.mmkvFloat(PlayerDefaults.LONG_PRESS_SPEED_TIMES)

    var allowResumePlayback: Boolean by SettingsStore.mmkvBool(true)

    var videoLanguage: String by SettingsStore.mmkvString("zhs")

    var videoQuality: String by SettingsStore.mmkvString("1080P")

    var showPlayedIndicator: Boolean by SettingsStore.mmkvBool(true)

    var searchArtistIgnoreVideoType: Boolean by SettingsStore.mmkvBool(false)

    // ---------------- 布局 ----------------

    var searchGridColumnsCompact: Int
            by SettingsStore.mmkvInt(SearchGridColumnsConfig.DEFAULT_COMPACT_COLUMNS)

    var searchGridColumnsMedium: Int
            by SettingsStore.mmkvInt(SearchGridColumnsConfig.DEFAULT_MEDIUM_COLUMNS)

    var searchGridColumnsExpanded: Int
            by SettingsStore.mmkvInt(SearchGridColumnsConfig.DEFAULT_EXPANDED_COLUMNS)

    var searchGridColumnsLarge: Int
            by SettingsStore.mmkvInt(SearchGridColumnsConfig.DEFAULT_LARGE_COLUMNS)

    var searchGridColumnsConfig: SearchGridColumnsConfig
        get() = SearchGridColumnsConfig(
            compactColumns = searchGridColumnsCompact,
            mediumColumns = searchGridColumnsMedium,
            expandedColumns = searchGridColumnsExpanded,
            largeColumns = searchGridColumnsLarge,
        )
        set(value) {
            searchGridColumnsCompact = value.compactColumns
            searchGridColumnsMedium = value.mediumColumns
            searchGridColumnsExpanded = value.expandedColumns
            searchGridColumnsLarge = value.largeColumns
        }

    var horizontalCardCountNarrow: Float
            by SettingsStore.mmkvFloat(HorizontalCardCountConfig.DEFAULT_NARROW_COUNT)

    var horizontalCardCountCompact: Float
            by SettingsStore.mmkvFloat(HorizontalCardCountConfig.DEFAULT_COMPACT_COUNT)

    var horizontalCardCountMedium: Float
            by SettingsStore.mmkvFloat(HorizontalCardCountConfig.DEFAULT_MEDIUM_COUNT)

    var horizontalCardCountExpanded: Float
            by SettingsStore.mmkvFloat(HorizontalCardCountConfig.DEFAULT_EXPANDED_COUNT)

    var horizontalCardCountConfig: HorizontalCardCountConfig
        get() = HorizontalCardCountConfig(
            narrowCount = horizontalCardCountNarrow,
            compactCount = horizontalCardCountCompact,
            mediumCount = horizontalCardCountMedium,
            expandedCount = horizontalCardCountExpanded,
        )
        set(value) {
            horizontalCardCountNarrow = value.narrowCount
            horizontalCardCountCompact = value.compactCount
            horizontalCardCountMedium = value.mediumCount
            horizontalCardCountExpanded = value.expandedCount
        }

    // ---------------- 网络 ----------------

    /** 用户在「域名」里选的站点，[baseUrl] 在没开自定义镜像时用它。 */
    var domainName: String by SettingsStore.mmkvString(HANIME_URL[0])

    var selectedBaseUrl: String by SettingsStore.mmkvString(HANIME_URL[0])

    var useCustomMirrorSite: Boolean by SettingsStore.mmkvBool(false)

    var customMirrorSite: String by SettingsStore.mmkvString(EMPTY_STRING)

    var appendCustomMirrorPath: Boolean by SettingsStore.mmkvBool(true)

    val baseUrl: String
        get() {
            if (useCustomMirrorSite && customMirrorSite.isNotBlank()) {
                val url = if (appendCustomMirrorPath) customMirrorSite else customMirrorSite.toRootUrl()
                return url.ensureTrailingSlash()
            }
            return domainName
        }

    val homeUrl: String
        get() {
            if (useCustomMirrorSite && customMirrorSite.isNotBlank()) return customMirrorSite
            return baseUrl
        }

    var useBuiltInHosts: Boolean by SettingsStore.mmkvBool(false)

    var customHostsData: String by SettingsStore.mmkvString(EMPTY_STRING)

    var useDoH: Boolean by SettingsStore.mmkvBool(false)

    var dohPreset: String by SettingsStore.mmkvString("alidns")

    var dohCustomUrl: String by SettingsStore.mmkvString(EMPTY_STRING)

    var dohBootstrapIps: String by SettingsStore.mmkvString(EMPTY_STRING)

    var dohTimeoutSeconds: Int by SettingsStore.mmkvInt(10)

    var proxyType: Int by SettingsStore.mmkvInt(ProxyType.SYSTEM)

    var proxyIp: String by SettingsStore.mmkvString(EMPTY_STRING)

    var proxyPort: Int by SettingsStore.mmkvInt(-1)

    // ---------------- 关键 H 帧 ----------------

    var whenCountdownRemindSec: Int by SettingsStore.mmkvInt(PlayerDefaults.COUNTDOWN_SEC)

    /** 毫秒。越不了界，最大就 30_000ms 而已。 */
    val whenCountdownRemind: Int get() = whenCountdownRemindSec * 1_000

    var showCommentWhenCountdown: Boolean by SettingsStore.mmkvBool(false)

    var hKeyframesEnable: Boolean by SettingsStore.mmkvBool(true)

    var sharedHKeyframesEnable: Boolean by SettingsStore.mmkvBool(true)

    var sharedHKeyframesUseFirst: Boolean by SettingsStore.mmkvBool(false)

    // ---------------- 下载 ----------------

    var downloadCountLimit: Int by SettingsStore.mmkvInt(DownloadDefaults.MAX_CONCURRENT_DOWNLOAD_DEF)

    /** 档位索引，对应关系见 [DownloadDefaults.SPEED_BYTES]。 */
    var downloadSpeedLimitIndex: Int by SettingsStore.mmkvInt(DownloadDefaults.NO_LIMIT_INDEX)

    /** 每秒字节数，0 表示不限速。 */
    val downloadSpeedLimit: Long
        get() = DownloadDefaults.SPEED_BYTES.getOrElse(downloadSpeedLimitIndex) {
            DownloadDefaults.NO_LIMIT
        }

    var collapseDownloadedGroup: Boolean by SettingsStore.mmkvBool(false)

    var isUsePrivateStorage: Boolean by SettingsStore.mmkvBool(true)

    var safDownloadPath: String? by SettingsStore.mmkvNullableString()

    // ---------------- MPV ----------------

    /** 预设模式 */
    var mpvProfile: String by SettingsStore.mmkvString("fast")

    /** gpu-next 渲染器 */
    var enableGPUNextRenderer: Boolean by SettingsStore.mmkvBool(false)

    /** 插帧相关 */
    var mpvInterpolation: Boolean by SettingsStore.mmkvBool(false)

    /** 去色带 */
    var mpvDeband: Boolean by SettingsStore.mmkvBool(true)

    /** GPU 繁忙时允许丢帧 */
    var mpvFramedrop: Boolean by SettingsStore.mmkvBool(true)

    /** 硬件解码 */
    var mpvHwdec: String by SettingsStore.mmkvString("Auto")

    /** 预缓存秒数 */
    var mpvCacheSecs: Int by SettingsStore.mmkvInt(60)

    /** 忽略证书验证 */
    var mpvTlsVerify: Boolean by SettingsStore.mmkvBool(true)

    /** 请求超时 */
    var mpvNetworkTimeout: Int by SettingsStore.mmkvInt(10)

    var customMpvParams: String by SettingsStore.mmkvString(EMPTY_STRING)

    // ---------------- 备份 ----------------

    /**
     * 导出给备份用的设置快照。
     *
     * 只含 [SettingsStore]——[AccountStore]（登录 cookie 等）和 [MiscStore] 从来就不在备份范围内，
     * 迁移到 MMKV 之前也是这样。
     *
     * 之所以要手写一遍而不是遍历 MMKV：MMKV **不保存值的类型信息**，`allKeys()` 只给 key、
     * 读取必须先知道类型。写在这里的好处是名单和属性声明在同一个文件，加设置项时不容易漏。
     */
    fun exportSettings(): Map<String, Any> = mapOf(
        ::usageNoticeAccepted.name to usageNoticeAccepted,
        ::savedUserId.name to savedUserId,
        ::appLanguage.name to appLanguage,
        ::useDarkMode.name to useDarkMode,
        ::useDynamicColor.name to useDynamicColor,
        ::fakeLauncherIcon.name to fakeLauncherIcon,
        ::tabletMode.name to tabletMode,
        ::disablePredictiveBack.name to disablePredictiveBack,
        ::disableMobileDataWarning.name to disableMobileDataWarning,
        ::disableComments.name to disableComments,
        ::useLockScreen.name to useLockScreen,
        ::allowPipMode.name to allowPipMode,
        ::isAnalyticsEnabled.name to isAnalyticsEnabled,
        ::homeCategoryOrder.name to homeCategoryOrder,
        ::homeCategoryHidden.name to homeCategoryHidden,
        ::updatePopupIntervalDays.name to updatePopupIntervalDays,
        ::useCIUpdateChannel.name to useCIUpdateChannel,
        ::switchPlayerKernel.name to switchPlayerKernel,
        ::showBottomProgress.name to showBottomProgress,
        ::playerSpeed.name to playerSpeed,
        ::slideSensitivity.name to slideSensitivity,
        ::longPressSpeedTime.name to longPressSpeedTime,
        ::allowResumePlayback.name to allowResumePlayback,
        ::videoLanguage.name to videoLanguage,
        ::videoQuality.name to videoQuality,
        ::showPlayedIndicator.name to showPlayedIndicator,
        ::searchArtistIgnoreVideoType.name to searchArtistIgnoreVideoType,
        ::searchGridColumnsCompact.name to searchGridColumnsCompact,
        ::searchGridColumnsMedium.name to searchGridColumnsMedium,
        ::searchGridColumnsExpanded.name to searchGridColumnsExpanded,
        ::searchGridColumnsLarge.name to searchGridColumnsLarge,
        ::horizontalCardCountNarrow.name to horizontalCardCountNarrow,
        ::horizontalCardCountCompact.name to horizontalCardCountCompact,
        ::horizontalCardCountMedium.name to horizontalCardCountMedium,
        ::horizontalCardCountExpanded.name to horizontalCardCountExpanded,
        ::domainName.name to domainName,
        ::selectedBaseUrl.name to selectedBaseUrl,
        ::useCustomMirrorSite.name to useCustomMirrorSite,
        ::customMirrorSite.name to customMirrorSite,
        ::appendCustomMirrorPath.name to appendCustomMirrorPath,
        ::useBuiltInHosts.name to useBuiltInHosts,
        ::customHostsData.name to customHostsData,
        ::useDoH.name to useDoH,
        ::dohPreset.name to dohPreset,
        ::dohCustomUrl.name to dohCustomUrl,
        ::dohBootstrapIps.name to dohBootstrapIps,
        ::dohTimeoutSeconds.name to dohTimeoutSeconds,
        ::proxyType.name to proxyType,
        ::proxyIp.name to proxyIp,
        ::proxyPort.name to proxyPort,
        ::whenCountdownRemindSec.name to whenCountdownRemindSec,
        ::showCommentWhenCountdown.name to showCommentWhenCountdown,
        ::hKeyframesEnable.name to hKeyframesEnable,
        ::sharedHKeyframesEnable.name to sharedHKeyframesEnable,
        ::sharedHKeyframesUseFirst.name to sharedHKeyframesUseFirst,
        ::downloadCountLimit.name to downloadCountLimit,
        ::downloadSpeedLimitIndex.name to downloadSpeedLimitIndex,
        ::collapseDownloadedGroup.name to collapseDownloadedGroup,
        ::isUsePrivateStorage.name to isUsePrivateStorage,
        ::mpvProfile.name to mpvProfile,
        ::enableGPUNextRenderer.name to enableGPUNextRenderer,
        ::mpvInterpolation.name to mpvInterpolation,
        ::mpvDeband.name to mpvDeband,
        ::mpvFramedrop.name to mpvFramedrop,
        ::mpvHwdec.name to mpvHwdec,
        ::mpvCacheSecs.name to mpvCacheSecs,
        ::mpvTlsVerify.name to mpvTlsVerify,
        ::mpvNetworkTimeout.name to mpvNetworkTimeout,
        ::customMpvParams.name to customMpvParams,
    ) + buildMap {
        // 两个可空项：没设置过就不进备份，免得把 null 写成空串。
        // 迁移前 `sp.all` 也是只导出写过的 key，行为一致。
        themeColor?.let { put(::themeColor.name, it) }
        safDownloadPath?.let { put(::safDownloadPath.name, it) }
    }

    /**
     * 恢复备份里的设置。类型直接取自备份文件里的值，所以不需要另外维护类型表。
     *
     * 认不出来的 key 会照原样写进 MMKV——反正没人读，和迁移前 `sp.all` 全量写回的行为一致。
     */
    fun importSettings(settings: Map<String, Any>) {
        val kv = SettingsStore.kv
        settings.forEach { (key, value) ->
            when (value) {
                is Boolean -> kv.set(key, value)
                is Int -> kv.set(key, value)
                is Long -> kv.set(key, value)
                is Float -> kv.set(key, value)
                is String -> kv.set(key, value)
            }
        }
    }
}

/**
 * [CookieString] 是 value class，MMKV 存不了，落盘时退化成里面的 String。
 * 这样 `loginCookieStateFlow` 对外仍是 `MutableStateFlow<CookieString>`，
 * `HCookieJar` 那几处调用点不用改。
 */
private fun MMKVOwner.mmkvCookie(key: String? = null) = MMKVProperty(
    owner = this,
    decode = { k, def -> CookieString(getString(k, def.cookie)) },
    encode = { k, v -> set(k, v.cookie) },
    defaultValue = CookieString(EMPTY_STRING),
    key = key,
)

private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"

/**
 * 取 `scheme://authority`，等价于原先的 `"${uri.scheme}://${uri.encodedAuthority}"`。
 * 用字符串切割而不是 `androidx.core.net.toUri`，因为要待在 commonMain。
 */
private fun String.toRootUrl(): String {
    val schemeEnd = indexOf("://")
    if (schemeEnd < 0) return this
    val authorityStart = schemeEnd + "://".length
    val end = indexOfAny(charArrayOf('/', '?', '#'), authorityStart)
    return if (end < 0) this else substring(0, end)
}
