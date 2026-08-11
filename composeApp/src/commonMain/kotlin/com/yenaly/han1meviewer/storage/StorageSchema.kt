package com.yenaly.han1meviewer.storage

import com.yenaly.han1meviewer.ui.navigation.settings.SettingsPreferenceKeys

/** Compile-time registry for every MMKV value owned by the application. */
object StorageSchema {
    object Auth {
        val alreadyLogin = key(
            owner = StorageOwnerId.Auth,
            name = "already_login",
            defaultValue = false,
            codec = StorageCodecs.boolean,
            legacySource = LegacyStorageSource.PackageName,
        )
        val loginCookie = key(
            owner = StorageOwnerId.Auth,
            name = "cookie",
            defaultValue = "",
            codec = StorageCodecs.string,
            legacySource = LegacyStorageSource.PackageName,
        )
        val cloudflareCookie = key(
            owner = StorageOwnerId.Auth,
            name = "cf_cookie",
            defaultValue = "",
            codec = StorageCodecs.string,
            legacySource = LegacyStorageSource.PackageName,
        )
        val savedUserId = key(
            owner = StorageOwnerId.Auth,
            name = "saved_user_id",
            defaultValue = "",
            codec = StorageCodecs.string,
            backupPolicy = StorageBackupPolicy.LegacyV1,
            legacySource = LegacyStorageSource.DefaultPreferences,
        )

        val keys: List<StorageKey<*>> =
            listOf(alreadyLogin, loginCookie, cloudflareCookie, savedUserId)
    }

    object Settings {
        private fun <T> setting(
            name: String,
            defaultValue: T,
            codec: StorageCodec<T>,
        ) = key(
            owner = StorageOwnerId.Settings,
            name = name,
            defaultValue = defaultValue,
            codec = codec,
            backupPolicy = StorageBackupPolicy.LegacyV1,
            legacySource = LegacyStorageSource.DefaultPreferences,
        )

        val appLanguage = setting("app_language", "system", StorageCodecs.string)
        val disableComments = setting("disable_comments", false, StorageCodecs.boolean)
        val useLockScreen = setting("use_lock_screen", false, StorageCodecs.boolean)

        val videoLanguage = setting(SettingsPreferenceKeys.VIDEO_LANGUAGE, "zhs", StorageCodecs.string)
        val defaultVideoQuality = setting(SettingsPreferenceKeys.DEFAULT_VIDEO_QUALITY, "1080P", StorageCodecs.string)
        val showPlayedIndicator = setting(SettingsPreferenceKeys.SHOW_PLAYED_INDICATOR, true, StorageCodecs.boolean)
        val updatePopupIntervalDays = setting(SettingsPreferenceKeys.UPDATE_POPUP_INTERVAL_DAYS, 0, StorageCodecs.int)
        val useCiUpdateChannel = setting(SettingsPreferenceKeys.USE_CI_UPDATE_CHANNEL, false, StorageCodecs.boolean)
        val useAnalytics = setting(SettingsPreferenceKeys.USE_ANALYTICS, true, StorageCodecs.boolean)
        val fakeLauncherIcon = setting(
            SettingsPreferenceKeys.FAKE_LAUNCHER_ICON,
            "com.yenaly.han1meviewer.LauncherAliasDefault",
            StorageCodecs.string,
        )
        val useDarkMode = setting(SettingsPreferenceKeys.USE_DARK_MODE, "always_off", StorageCodecs.string)
        val useDynamicColor = setting(SettingsPreferenceKeys.USE_DYNAMIC_COLOR, false, StorageCodecs.boolean)
        val themeColor = setting(SettingsPreferenceKeys.THEME_COLOR, null, StorageCodecs.nullableString)
        val allowResumePlayback = setting(SettingsPreferenceKeys.ALLOW_RESUME_PLAYBACK, true, StorageCodecs.boolean)
        val allowPipMode = setting(SettingsPreferenceKeys.ALLOW_PIP_MODE, false, StorageCodecs.boolean)
        val searchArtistIgnoreVideoType = setting(
            SettingsPreferenceKeys.SEARCH_ARTIST_IGNORE_VIDEO_TYPE,
            false,
            StorageCodecs.boolean,
        )
        val disableMobileDataWarning = setting(
            SettingsPreferenceKeys.DISABLE_MOBILE_DATA_WARNING,
            false,
            StorageCodecs.boolean,
        )
        val collapseDownloadedGroup = setting(
            SettingsPreferenceKeys.COLLAPSE_DOWNLOADED_GROUP,
            false,
            StorageCodecs.boolean,
        )
        val disablePredictiveBack = setting(
            SettingsPreferenceKeys.DISABLE_PREDICTIVE_BACK,
            false,
            StorageCodecs.boolean,
        )
        val tabletMode = setting(SettingsPreferenceKeys.TABLET_MODE, false, StorageCodecs.boolean)

        val searchGridColumnsCompact = setting(SettingsPreferenceKeys.SEARCH_GRID_COLUMNS_COMPACT, 2, StorageCodecs.int)
        val searchGridColumnsMedium = setting(SettingsPreferenceKeys.SEARCH_GRID_COLUMNS_MEDIUM, 3, StorageCodecs.int)
        val searchGridColumnsExpanded = setting(SettingsPreferenceKeys.SEARCH_GRID_COLUMNS_EXPANDED, 4, StorageCodecs.int)
        val searchGridColumnsLarge = setting(SettingsPreferenceKeys.SEARCH_GRID_COLUMNS_LARGE, 5, StorageCodecs.int)
        val horizontalCardCountNarrow = setting(
            SettingsPreferenceKeys.HORIZONTAL_CARD_COUNT_NARROW,
            "1.5",
            StorageCodecs.string,
        )
        val horizontalCardCountCompact = setting(
            SettingsPreferenceKeys.HORIZONTAL_CARD_COUNT_COMPACT,
            "2.1",
            StorageCodecs.string,
        )
        val horizontalCardCountMedium = setting(
            SettingsPreferenceKeys.HORIZONTAL_CARD_COUNT_MEDIUM,
            "4.1",
            StorageCodecs.string,
        )
        val horizontalCardCountExpanded = setting(
            SettingsPreferenceKeys.HORIZONTAL_CARD_COUNT_EXPANDED,
            "5.1",
            StorageCodecs.string,
        )
        val homeCategoryOrder = setting(SettingsPreferenceKeys.HOME_CATEGORY_ORDER, null, StorageCodecs.nullableString)
        val homeCategoryHidden = setting(SettingsPreferenceKeys.HOME_CATEGORY_HIDDEN, null, StorageCodecs.nullableString)

        val showBottomProgress = setting(SettingsPreferenceKeys.SHOW_BOTTOM_PROGRESS, true, StorageCodecs.boolean)
        val switchPlayerKernel = setting(SettingsPreferenceKeys.SWITCH_PLAYER_KERNEL, "Media3", StorageCodecs.string)
        val playerSpeed = setting(SettingsPreferenceKeys.PLAYER_SPEED, "1.0", StorageCodecs.string)
        val slideSensitivity = setting(SettingsPreferenceKeys.SLIDE_SENSITIVITY, 5, StorageCodecs.int)
        val longPressSpeedTimes = setting(SettingsPreferenceKeys.LONG_PRESS_SPEED_TIMES, "2.5", StorageCodecs.string)

        val domainName = setting(SettingsPreferenceKeys.DOMAIN_NAME, DEFAULT_BASE_URL, StorageCodecs.string)
        val selectedBaseUrl = setting(SettingsPreferenceKeys.SELECTED_BASE_URL, DEFAULT_BASE_URL, StorageCodecs.string)
        val useCustomMirrorSite = setting(SettingsPreferenceKeys.USE_CUSTOM_MIRROR_SITE, false, StorageCodecs.boolean)
        val customMirrorSite = setting(SettingsPreferenceKeys.CUSTOM_MIRROR_SITE, "", StorageCodecs.string)
        val appendCustomMirrorPath = setting(SettingsPreferenceKeys.APPEND_CUSTOM_MIRROR_PATH, true, StorageCodecs.boolean)
        val useBuiltInHosts = setting(SettingsPreferenceKeys.USE_BUILT_IN_HOSTS, false, StorageCodecs.boolean)
        val customHostsData = setting(SettingsPreferenceKeys.CUSTOM_HOSTS_DATA, "", StorageCodecs.string)
        val useDoh = setting(SettingsPreferenceKeys.USE_DOH, false, StorageCodecs.boolean)
        val dohPreset = setting(SettingsPreferenceKeys.DOH_PRESET, "alidns", StorageCodecs.string)
        val dohCustomUrl = setting(SettingsPreferenceKeys.DOH_CUSTOM_URL, "", StorageCodecs.string)
        val dohBootstrapIps = setting(SettingsPreferenceKeys.DOH_BOOTSTRAP_IPS, "", StorageCodecs.string)
        val dohTimeoutSeconds = setting(SettingsPreferenceKeys.DOH_TIMEOUT_SECONDS, 10, StorageCodecs.int)
        val proxyType = setting(SettingsPreferenceKeys.PROXY_TYPE, 1, StorageCodecs.int)
        val proxyIp = setting(SettingsPreferenceKeys.PROXY_IP, "", StorageCodecs.string)
        val proxyPort = setting(SettingsPreferenceKeys.PROXY_PORT, -1, StorageCodecs.int)

        val downloadCountLimit = setting(SettingsPreferenceKeys.DOWNLOAD_COUNT_LIMIT, 2, StorageCodecs.int)
        val downloadSpeedLimit = setting(
            SettingsPreferenceKeys.DOWNLOAD_SPEED_LIMIT,
            0,
            StorageCodecs.intRange(minimum = 0, maximum = 8),
        )
        val usePrivateStorage = setting(SettingsPreferenceKeys.USE_PRIVATE_STORAGE, true, StorageCodecs.boolean)
        val safDownloadPath = setting("saf_download_path", null, StorageCodecs.nullableString)

        val whenCountdownRemind = setting(SettingsPreferenceKeys.WHEN_COUNTDOWN_REMIND, 10, StorageCodecs.int)
        val showCommentWhenCountdown = setting(
            SettingsPreferenceKeys.SHOW_COMMENT_WHEN_COUNTDOWN,
            false,
            StorageCodecs.boolean,
        )
        val hKeyframesEnable = setting(SettingsPreferenceKeys.H_KEYFRAMES_ENABLE, true, StorageCodecs.boolean)
        val sharedHKeyframesEnable = setting(
            SettingsPreferenceKeys.SHARED_H_KEYFRAMES_ENABLE,
            true,
            StorageCodecs.boolean,
        )
        val sharedHKeyframesUseFirst = setting(
            SettingsPreferenceKeys.SHARED_H_KEYFRAMES_USE_FIRST,
            false,
            StorageCodecs.boolean,
        )

        val mpvProfile = setting(SettingsPreferenceKeys.MPV_PROFILE, "fast", StorageCodecs.string)
        val enableGpuNextRenderer = setting(
            SettingsPreferenceKeys.ENABLE_GPU_NEXT_RENDERER,
            false,
            StorageCodecs.boolean,
        )
        val mpvInterpolation = setting(SettingsPreferenceKeys.MPV_INTERPOLATION, false, StorageCodecs.boolean)
        val mpvDeband = setting(SettingsPreferenceKeys.MPV_DEBAND, true, StorageCodecs.boolean)
        val mpvFramedrop = setting(SettingsPreferenceKeys.MPV_FRAMEDROP, true, StorageCodecs.boolean)
        val mpvHwdec = setting(SettingsPreferenceKeys.MPV_HWDEC, "Auto", StorageCodecs.string)
        val mpvCacheSecs = setting(SettingsPreferenceKeys.MPV_CACHE_SECS, 60, StorageCodecs.int)
        val mpvTlsVerify = setting(SettingsPreferenceKeys.MPV_TLS_VERIFY, true, StorageCodecs.boolean)
        val mpvNetworkTimeout = setting(SettingsPreferenceKeys.MPV_NETWORK_TIMEOUT, 10, StorageCodecs.int)
        val customMpvParams = setting(SettingsPreferenceKeys.CUSTOM_PARAMS, "", StorageCodecs.string)

        val keys: List<StorageKey<*>> = listOf(
            appLanguage,
            disableComments,
            useLockScreen,
            videoLanguage,
            defaultVideoQuality,
            showPlayedIndicator,
            updatePopupIntervalDays,
            useCiUpdateChannel,
            useAnalytics,
            fakeLauncherIcon,
            useDarkMode,
            useDynamicColor,
            themeColor,
            allowResumePlayback,
            allowPipMode,
            searchArtistIgnoreVideoType,
            disableMobileDataWarning,
            collapseDownloadedGroup,
            disablePredictiveBack,
            tabletMode,
            searchGridColumnsCompact,
            searchGridColumnsMedium,
            searchGridColumnsExpanded,
            searchGridColumnsLarge,
            horizontalCardCountNarrow,
            horizontalCardCountCompact,
            horizontalCardCountMedium,
            horizontalCardCountExpanded,
            homeCategoryOrder,
            homeCategoryHidden,
            showBottomProgress,
            switchPlayerKernel,
            playerSpeed,
            slideSensitivity,
            longPressSpeedTimes,
            domainName,
            selectedBaseUrl,
            useCustomMirrorSite,
            customMirrorSite,
            appendCustomMirrorPath,
            useBuiltInHosts,
            customHostsData,
            useDoh,
            dohPreset,
            dohCustomUrl,
            dohBootstrapIps,
            dohTimeoutSeconds,
            proxyType,
            proxyIp,
            proxyPort,
            downloadCountLimit,
            downloadSpeedLimit,
            usePrivateStorage,
            safDownloadPath,
            whenCountdownRemind,
            showCommentWhenCountdown,
            hKeyframesEnable,
            sharedHKeyframesEnable,
            sharedHKeyframesUseFirst,
            mpvProfile,
            enableGpuNextRenderer,
            mpvInterpolation,
            mpvDeband,
            mpvFramedrop,
            mpvHwdec,
            mpvCacheSecs,
            mpvTlsVerify,
            mpvNetworkTimeout,
            customMpvParams,
        )

        private const val DEFAULT_BASE_URL = "https://hanime1.me/"
    }

    object UiState {
        val usageNoticeAccepted = key(
            owner = StorageOwnerId.UiState,
            name = "usage_notice_accepted",
            defaultValue = false,
            codec = StorageCodecs.boolean,
            backupPolicy = StorageBackupPolicy.LegacyV1,
            legacySource = LegacyStorageSource.DefaultPreferences,
        )
        val updateNodeId = key(
            owner = StorageOwnerId.UiState,
            name = "update_node_id",
            defaultValue = "",
            codec = StorageCodecs.string,
            legacySource = LegacyStorageSource.PackageName,
        )
        val lastUpdatePopupTime = key(
            owner = StorageOwnerId.UiState,
            name = SettingsPreferenceKeys.LAST_UPDATE_POPUP_TIME,
            defaultValue = 0L,
            codec = StorageCodecs.long,
            legacySource = LegacyStorageSource.PackageName,
        )
        val lastDismissTime = key(
            owner = StorageOwnerId.UiState,
            name = "last_dismiss_time",
            defaultValue = 0L,
            codec = StorageCodecs.long,
            legacySource = LegacyStorageSource.SettingPreferences,
        )

        val keys: List<StorageKey<*>> =
            listOf(usageNoticeAccepted, updateNodeId, lastUpdatePopupTime, lastDismissTime)
    }

    object MigrationMeta {
        val sharedPreferencesV1 = key(
            owner = StorageOwnerId.MigrationMeta,
            name = "migration.shared_preferences.v1",
            defaultValue = false,
            codec = StorageCodecs.boolean,
        )

        val keys: List<StorageKey<*>> = listOf(sharedPreferencesV1)
    }

    val allKeys: List<StorageKey<*>> by lazy {
        Auth.keys + Settings.keys + UiState.keys + MigrationMeta.keys
    }

    private val keysByOwnerAndName: Map<Pair<StorageOwnerId, String>, StorageKey<*>> by lazy {
        allKeys.associateBy { it.owner to it.name }.also { registry ->
            check(registry.size == allKeys.size) { "Storage schema contains duplicate owner/name pairs" }
        }
    }

    fun keys(owner: StorageOwnerId): List<StorageKey<*>> = allKeys.filter { it.owner == owner }

    fun findKey(owner: StorageOwnerId, name: String): StorageKey<*>? =
        keysByOwnerAndName[owner to name]

    fun findKeys(name: String): List<StorageKey<*>> = allKeys.filter { it.name == name }

    fun findLegacyV1Key(name: String): StorageKey<*>? =
        legacyV1KeysByName[name]

    fun requireKey(owner: StorageOwnerId, name: String): StorageKey<*> =
        requireNotNull(findKey(owner, name)) { "Unknown storage key ${owner.name}:$name" }

    private val legacyV1KeysByName: Map<String, StorageKey<*>> by lazy {
        allKeys.filter { it.backupPolicy == StorageBackupPolicy.LegacyV1 }
            .associateBy(StorageKey<*>::name)
            .also { registry ->
                check(registry.size == allKeys.count { it.backupPolicy == StorageBackupPolicy.LegacyV1 }) {
                    "Legacy-v1 backup keys must be globally unique"
                }
            }
    }

    private fun <T> key(
        owner: StorageOwnerId,
        name: String,
        defaultValue: T,
        codec: StorageCodec<T>,
        backupPolicy: StorageBackupPolicy = StorageBackupPolicy.Excluded,
        legacySource: LegacyStorageSource = LegacyStorageSource.None,
    ): StorageKey<T> = StorageKey(
        owner = owner,
        name = name,
        defaultProvider = { defaultValue },
        codec = codec,
        backupPolicy = backupPolicy,
        legacySource = legacySource,
    )
}
