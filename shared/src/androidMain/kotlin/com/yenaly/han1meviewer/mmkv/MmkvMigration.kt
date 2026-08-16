package com.yenaly.han1meviewer.mmkv

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import co.touchlab.kermit.Logger

/**
 * 标记位存在 [SettingsStore] 里。它不是配置项，不会被 `Preferences.exportSettings()` 导出。
 */
private const val MIGRATED_FLAG = "__migratedFromSharedPreferences"

private const val LEGACY_MISC_SP_NAME = "setting_pref"

private val logger = Logger.withTag("MmkvMigration")

/**
 * 迁移到 MMKV 之前，配置项的 key 是下划线字符串；之后统一用 Kotlin 属性名。
 * 这张表只服务于「读旧数据」，**写死不动**——以后新增设置项不用往这里加。
 *
 * 等某个版本确认所有用户都升上来了，这个文件连同 `com.tencent:mmkv` 依赖可以整个删掉。
 */
internal object LegacyPreferenceKeys {

    /** 原 `<packageName>_preferences`。[BackupManager][com.yenaly.han1meviewer.logic.BackupManager] 恢复旧备份文件时也要用。 */
    val settings: Map<String, String> = mapOf(
        "usage_notice_accepted" to "usageNoticeAccepted",
        "saved_user_id" to "savedUserId",
        "app_language" to "appLanguage",
        "use_dark_mode" to "useDarkMode",
        "use_dynamic_color" to "useDynamicColor",
        "theme_color" to "themeColor",
        "pref_fake_launcher_icon" to "fakeLauncherIcon",
        "tablet_mode" to "tabletMode",
        "disable_predictive_back" to "disablePredictiveBack",
        "disable_mobile_data_warning" to "disableMobileDataWarning",
        "disable_comments" to "disableComments",
        "use_lock_screen" to "useLockScreen",
        "allow_pip_mode" to "allowPipMode",
        "use_analytics" to "isAnalyticsEnabled",
        "home_category_order" to "homeCategoryOrder",
        "home_category_hidden" to "homeCategoryHidden",
        "update_popup_interval_days" to "updatePopupIntervalDays",
        "use_ci_update_channel" to "useCIUpdateChannel",
        "switch_player_kernel" to "switchPlayerKernel",
        "show_bottom_progress" to "showBottomProgress",
        "player_speed" to "playerSpeed",
        "slide_sensitivity" to "slideSensitivity",
        "long_press_speed_times" to "longPressSpeedTime",
        "allow_resume_playback" to "allowResumePlayback",
        "video_language" to "videoLanguage",
        "default_video_quality" to "videoQuality",
        "show_played_indicator" to "showPlayedIndicator",
        "search_artist_ignore_video_type" to "searchArtistIgnoreVideoType",
        "search_grid_columns_compact" to "searchGridColumnsCompact",
        "search_grid_columns_medium" to "searchGridColumnsMedium",
        "search_grid_columns_expanded" to "searchGridColumnsExpanded",
        "search_grid_columns_large" to "searchGridColumnsLarge",
        "horizontal_card_count_narrow" to "horizontalCardCountNarrow",
        "horizontal_card_count_compact" to "horizontalCardCountCompact",
        "horizontal_card_count_medium" to "horizontalCardCountMedium",
        "horizontal_card_count_expanded" to "horizontalCardCountExpanded",
        "domain_name" to "domainName",
        "selectedBaseUrl" to "selectedBaseUrl",
        "use_custom_mirror_site" to "useCustomMirrorSite",
        "custom_mirror_site" to "customMirrorSite",
        "append_custom_mirror_path" to "appendCustomMirrorPath",
        "use_built_in_hosts" to "useBuiltInHosts",
        "custom_hosts_data" to "customHostsData",
        "use_doh" to "useDoH",
        "doh_preset" to "dohPreset",
        "doh_custom_url" to "dohCustomUrl",
        "doh_bootstrap_ips" to "dohBootstrapIps",
        "doh_timeout_seconds" to "dohTimeoutSeconds",
        "proxy_type" to "proxyType",
        "proxy_ip" to "proxyIp",
        "proxy_port" to "proxyPort",
        "when_countdown_remind" to "whenCountdownRemindSec",
        "show_comment_when_countdown" to "showCommentWhenCountdown",
        "h_keyframes_enable" to "hKeyframesEnable",
        "shared_h_keyframes_enable" to "sharedHKeyframesEnable",
        "shared_h_keyframes_use_first" to "sharedHKeyframesUseFirst",
        "download_count_limit" to "downloadCountLimit",
        "download_speed_limit" to "downloadSpeedLimitIndex",
        "collapse_downloaded_group" to "collapseDownloadedGroup",
        "use_private_storage" to "isUsePrivateStorage",
        "saf_download_path" to "safDownloadPath",
        "mpv_profile" to "mpvProfile",
        "mpv_gpu_next_render" to "enableGPUNextRenderer",
        "mpv_interpolation" to "mpvInterpolation",
        "mpv_deband" to "mpvDeband",
        "mpv_framedrop" to "mpvFramedrop",
        "mpv_hwdecx" to "mpvHwdec",
        "mpv_cache_secs" to "mpvCacheSecs",
        "mpv_tls_verify" to "mpvTlsVerify",
        "mpv_network_timeout" to "mpvNetworkTimeout",
        "mpv_custom_parameters" to "customMpvParams",
    )

    /** 原 `<packageName>`，也就是 yenaly_libs `getSpValue`/`putSpValue` 的默认文件。 */
    val account: Map<String, String> = mapOf(
        "already_login" to "isAlreadyLogin",
        "cookie" to "loginCookie",
        "cf_cookie" to "cloudFlareCookie",
        "update_node_id" to "updateNodeId",
        "last_update_popup_time" to "lastUpdatePopupTime",
    )

    /** 原 `setting_pref`。 */
    val misc: Map<String, String> = mapOf(
        "last_dismiss_time" to "lastDismissTime",
    )

    /**
     * 这几项旧库里是以 String 存的数值，属性类型现在是 Float，迁移时要转一道。
     */
    val stringToFloat: Set<String> = setOf(
        "player_speed",
        "long_press_speed_times",
        "horizontal_card_count_narrow",
        "horizontal_card_count_compact",
        "horizontal_card_count_medium",
        "horizontal_card_count_expanded",
    )
}

/**
 * 把三个旧 SharedPreferences 文件一次性搬进 MMKV。
 *
 * 不能用 `MMKV.importFromSharedPreferences`——它是原样复制 key 的，换不了名字。
 *
 * **旧 SP 文件不删**，留一版作回滚余地。
 */
fun migrateSharedPreferencesToMMKV(context: Context) {
    if (SettingsStore.kv.getBoolean(MIGRATED_FLAG, false)) return

    val migrated = migrate(
        sp = PreferenceManager.getDefaultSharedPreferences(context),
        owner = SettingsStore,
        keys = LegacyPreferenceKeys.settings,
    ) + migrate(
        sp = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE),
        owner = AccountStore,
        keys = LegacyPreferenceKeys.account,
    ) + migrate(
        sp = context.getSharedPreferences(LEGACY_MISC_SP_NAME, Context.MODE_PRIVATE),
        owner = MiscStore,
        keys = LegacyPreferenceKeys.misc,
    )

    SettingsStore.kv.set(MIGRATED_FLAG, true)
    logger.i { "migrated $migrated entries from SharedPreferences to MMKV" }
}

private fun migrate(sp: SharedPreferences, owner: MMKVOwner, keys: Map<String, String>): Int {
    var count = 0
    sp.all.forEach { (legacyKey, value) ->
        val name = keys[legacyKey] ?: return@forEach
        if (value == null) return@forEach
        val written = if (legacyKey in LegacyPreferenceKeys.stringToFloat) {
            (value as? String)?.toFloatOrNull()?.let { owner.kv.set(name, it) }
        } else when (value) {
            is Boolean -> owner.kv.set(name, value)
            is Int -> owner.kv.set(name, value)
            is Long -> owner.kv.set(name, value)
            is Float -> owner.kv.set(name, value)
            is String -> owner.kv.set(name, value)
            else -> null
        }
        if (written == true) {
            count++
        } else {
            logger.w { "cannot migrate \"$legacyKey\" -> $name: unexpected value $value" }
        }
    }
    return count
}
