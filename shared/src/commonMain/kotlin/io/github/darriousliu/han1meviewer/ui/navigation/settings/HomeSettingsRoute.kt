@file:OptIn(ExperimentalTime::class)

package io.github.darriousliu.han1meviewer.ui.navigation.settings

import io.github.darriousliu.han1meviewer.util.appCacheSizeBytes
import io.github.darriousliu.han1meviewer.util.clearAppCache
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darriousliu.han1meviewer.BuildConfig
import io.github.darriousliu.han1meviewer.HA1_GITHUB_FORUM_URL
import io.github.darriousliu.han1meviewer.HA1_GITHUB_ISSUE_URL
import io.github.darriousliu.han1meviewer.HanimeConstants
import io.github.darriousliu.han1meviewer.Preferences
import io.github.darriousliu.han1meviewer.logic.BackupManager
import io.github.darriousliu.han1meviewer.logic.state.WebsiteState
import io.github.darriousliu.han1meviewer.ui.component.ConfirmDialog
import io.github.darriousliu.han1meviewer.ui.component.LocalToaster
import io.github.darriousliu.han1meviewer.ui.component.showShort
import io.github.darriousliu.han1meviewer.ui.screen.home.homepage.defaultHomeCategoryPreferenceItems
import io.github.darriousliu.han1meviewer.ui.screen.home.homepage.hiddenHomeCategoryKeys
import io.github.darriousliu.han1meviewer.ui.screen.home.homepage.homeCategoryOrder
import io.github.darriousliu.han1meviewer.ui.screen.home.homepage.saveHomeCategoryPreferences
import io.github.darriousliu.han1meviewer.ui.screen.settings.HomeSettingsScreen
import io.github.darriousliu.han1meviewer.ui.screen.settings.dialog.AnalyticsConsentDialog
import io.github.darriousliu.han1meviewer.ui.screen.settings.dialog.FakeLauncherIconDialog
import io.github.darriousliu.han1meviewer.ui.screen.settings.dialog.LauncherIconOption
import io.github.darriousliu.han1meviewer.ui.screen.settings.dialog.LicenseDialog
import io.github.darriousliu.han1meviewer.ui.screen.settings.dialog.launcherIconOptions
import io.github.darriousliu.han1meviewer.ui.screen.settings.model.HomeSettingsUiState
import io.github.darriousliu.han1meviewer.ui.theme.ThemeColorPreset
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.action_app_open_by_default_settings_not_support
import io.github.darriousliu.han1meviewer.core.resource.already_latest_update
import io.github.darriousliu.han1meviewer.core.resource.always_off
import io.github.darriousliu.han1meviewer.core.resource.always_on
import io.github.darriousliu.han1meviewer.core.resource.attention
import io.github.darriousliu.han1meviewer.core.resource.backup_export_failed
import io.github.darriousliu.han1meviewer.core.resource.backup_export_success
import io.github.darriousliu.han1meviewer.core.resource.backup_import_confirm_message
import io.github.darriousliu.han1meviewer.core.resource.backup_import_failed
import io.github.darriousliu.han1meviewer.core.resource.backup_import_success
import io.github.darriousliu.han1meviewer.core.resource.backup_import_title
import io.github.darriousliu.han1meviewer.core.resource.cache_empty
import io.github.darriousliu.han1meviewer.core.resource.cancel
import io.github.darriousliu.han1meviewer.core.resource.check_update_failed
import io.github.darriousliu.han1meviewer.core.resource.check_update_success
import io.github.darriousliu.han1meviewer.core.resource.checking_update
import io.github.darriousliu.han1meviewer.core.resource.clear_failed
import io.github.darriousliu.han1meviewer.core.resource.clear_success
import io.github.darriousliu.han1meviewer.core.resource.confirm
import io.github.darriousliu.han1meviewer.core.resource.current_version
import io.github.darriousliu.han1meviewer.core.resource.english_lang
import io.github.darriousliu.han1meviewer.core.resource.fake_icon_hint
import io.github.darriousliu.han1meviewer.core.resource.follow_system
import io.github.darriousliu.han1meviewer.core.resource.japanese_lang
import io.github.darriousliu.han1meviewer.core.resource.not_compact_lock_screen
import io.github.darriousliu.han1meviewer.core.resource.not_set_sys_lock
import io.github.darriousliu.han1meviewer.core.resource.request_pip_alert
import io.github.darriousliu.han1meviewer.core.resource.restart_needed
import io.github.darriousliu.han1meviewer.core.resource.simplified_chinese
import io.github.darriousliu.han1meviewer.core.resource.success_value
import io.github.darriousliu.han1meviewer.core.resource.sure_to_clear
import io.github.darriousliu.han1meviewer.core.resource.sure_to_clear_cache
import io.github.darriousliu.han1meviewer.core.resource.traditional_chinese
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSettingsRouteScreen(
    onNavigateToPlayerSettings: () -> Unit,
    onNavigateToHKeyframeSettings: () -> Unit,
    onNavigateToDownloadSettings: () -> Unit,
    onNavigateToNetworkSettings: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val uriHandler = LocalUriHandler.current
    val actions = rememberHomeSettingsActions()
    // 下面这些在非组合的回调里用，stringResource 是 composable，得先解开
    val exportSuccess = stringResource(Res.string.backup_export_success)
    val exportFailed = stringResource(Res.string.backup_export_failed)
    val importSuccess = stringResource(Res.string.backup_import_success)
    val importFailed = stringResource(Res.string.backup_import_failed)
    val pipAlert = stringResource(Res.string.request_pip_alert)
    val notSetSysLock = stringResource(Res.string.not_set_sys_lock)
    val notCompactLockScreen = stringResource(Res.string.not_compact_lock_screen)
    val deepLinksNotSupported =
        stringResource(Res.string.action_app_open_by_default_settings_not_support)
    val cacheEmpty = stringResource(Res.string.cache_empty)
    val clearSuccess = stringResource(Res.string.clear_success)
    val clearFailed = stringResource(Res.string.clear_failed)
    val fakeIconHint = stringResource(Res.string.fake_icon_hint)
    val successValue = stringResource(Res.string.success_value)
    val versionState by actions.versionFlow.collectAsStateWithLifecycle()
    var refreshKey by remember { mutableIntStateOf(0) }
    var cacheKey by remember { mutableIntStateOf(0) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showLicenseScreen by remember { mutableStateOf(false) }
    var showRestartConfirmDialog by remember { mutableStateOf(false) }
    var showAnalyticsDialog by remember { mutableStateOf(false) }
    var showLauncherPicker by remember { mutableStateOf(false) }
    var pendingImportFile by remember { mutableStateOf<PlatformFile?>(null) }

    // BackupManager.exportTo/importFrom 本来就吃 PlatformFile 且是 suspend，
    // FileKit 自己管调度，所以这里不用再套 Dispatchers.IO
    val exportLauncher = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
    ) { file ->
        file ?: return@rememberFileSaverLauncher
        coroutineScope.launch {
            runCatching { BackupManager.exportTo(file) }
                .onSuccess { toaster.showShort(exportSuccess) }
                .onFailure { toaster.showShort(exportFailed) }
        }
    }
    // 不写 FileKitType.File("json")：现在的过滤是 arrayOf("application/json",
    // "text/*", "*/*")，那个 */* 等于「任意文件」。收紧成 json 会让 Downloads 里
    // 被 provider 报成 application/octet-stream 的备份选不中
    val importLauncher = rememberFilePickerLauncher(type = FileKitType.File()) { file ->
        pendingImportFile = file
    }

    // 算目录大小是 IO，拼文案是资源——摘要文案现在是 @Composable（commonMain 的
    // SettingsSummaries），不能在协程里调，所以这里只留大小，文案在下面组合时拼
    var cacheSize by remember { mutableLongStateOf(0L) }

    LaunchedEffect(cacheKey) {
        cacheSize = appCacheSizeBytes()
    }
    val cacheSummary = generateClearCacheSummary(cacheSize)
    val checkUpdateFailed = stringResource(Res.string.check_update_failed)
    val checkingUpdate = stringResource(Res.string.checking_update)
    val alreadyLatestUpdate = stringResource(Res.string.already_latest_update)

    val updateSummary = run {
        when (versionState) {
            is WebsiteState.Error -> checkUpdateFailed
            is WebsiteState.Loading -> checkingUpdate
            is WebsiteState.Success -> {
                val info = (versionState as WebsiteState.Success).info
                if (info == null) {
                    alreadyLatestUpdate
                } else {
                    stringResource(Res.string.check_update_success, info.version)
                }
            }
        }
    }
    val themeColorName = stringResource(ThemeColorPreset.fromKey(Preferences.themeColor).displayNameRes)
    val uiState = buildHomeSettingsUiState(
        launcherOptions = launcherIconOptions,
        updateSummary = updateSummary,
        cacheSummary = cacheSummary,
        themeColorName = themeColorName,
        refreshKey = refreshKey,
    )

    HomeSettingsScreen(
        state = uiState,
        onVideoLanguageChange = { value ->
            if (value != Preferences.videoLanguage) {
                Preferences.videoLanguage = value
                showRestartConfirmDialog = true
            }
        },
        onVideoQualityChange = { value ->
            Preferences.videoQuality = value
            refreshKey++
            // 在回调里，stringResource(res, vararg) 这种 composable API 用不了，
            // 只能拿解好的模板自己替。`success_value` 四种语言都是 `%1$s`（核实过）
            toaster.showShort(successValue.replace("%1\$s", value))
        },
        onDarkModeChange = { value ->
            if (value != Preferences.useDarkMode) {
                Preferences.useDarkMode = value
                actions.applyDarkMode()
            }
        },
        onAllowPipModeChange = { enabled ->
            if (enabled && !actions.isPipPermissionGranted()) {
                toaster.showShort(pipAlert)
                actions.openPipPermissionSettings()
                Preferences.allowPipMode = false
                refreshKey++
                return@HomeSettingsScreen
            }
            Preferences.allowPipMode = enabled
            refreshKey++
        },
        onAllowResumePlaybackChange = {
            Preferences.allowResumePlayback = it
            refreshKey++
        },
        onShowPlayedIndicatorChange = {
            Preferences.showPlayedIndicator = it
            refreshKey++
        },
        onSearchArtistIgnoreVideoTypeChange = {
            Preferences.searchArtistIgnoreVideoType = it
            refreshKey++
        },
        onDisableMobileDataWarningChange = {
            Preferences.disableMobileDataWarning = it
            refreshKey++
        },
        onDisablePredictiveBackChange = {
            Preferences.disablePredictiveBack = it
            refreshKey++
        },
        onTabletModeChange = {
            Preferences.tabletMode = it
            refreshKey++
        },
        onDisableCommentsChange = {
            Preferences.disableComments = it
            refreshKey++
        },
        onCollapseDownloadedGroupChange = {
            Preferences.collapseDownloadedGroup = it
            refreshKey++
        },
        onSearchGridColumnsConfigChange = { config ->
            Preferences.searchGridColumnsCompact = config.compactColumns
            Preferences.searchGridColumnsMedium = config.mediumColumns
            Preferences.searchGridColumnsExpanded = config.expandedColumns
            Preferences.searchGridColumnsLarge = config.largeColumns
            refreshKey++
        },
        onHorizontalCardCountConfigChange = { config ->
            Preferences.horizontalCardCountNarrow = config.narrowCount
            Preferences.horizontalCardCountCompact = config.compactCount
            Preferences.horizontalCardCountMedium = config.mediumCount
            Preferences.horizontalCardCountExpanded = config.expandedCount
            refreshKey++
        },
        onThemeColorChange = { key ->
            Preferences.themeColor = key
            refreshKey++
            actions.applyThemeColor()
        },
        onHomeCategoryPreferencesChange = { order, hiddenKeys ->
            saveHomeCategoryPreferences(order, hiddenKeys)
            refreshKey++
        },
        onUseCIUpdateChannelChange = { value ->
            Preferences.useCIUpdateChannel = value
            refreshKey++
            actions.onUpdateChannelChanged()
        },
        onUseAnalyticsChange = { value ->
            if (!value) {
                showAnalyticsDialog = true
                return@HomeSettingsScreen
            }
            Preferences.isAnalyticsEnabled = true
            refreshKey++
            actions.setAnalyticsEnabled(true)
        },
        onUseLockScreenChange = { value ->
            if (value) {
                when (actions.deviceLockAvailability()) {
                    DeviceLockAvailability.NoSystemLock -> {
                        toaster.showShort(notSetSysLock)
                        refreshKey++
                        return@HomeSettingsScreen
                    }

                    DeviceLockAvailability.UnsupportedOsVersion,
                    DeviceLockAvailability.Unsupported -> {
                        toaster.showShort(notCompactLockScreen)
                        refreshKey++
                        return@HomeSettingsScreen
                    }

                    DeviceLockAvailability.Available -> Unit
                }
            }
            Preferences.useLockScreen = value
            refreshKey++
        },
        onOpenPlayerSettings = onNavigateToPlayerSettings,
        onOpenHKeyframeSettings = onNavigateToHKeyframeSettings,
        onOpenDownloadSettings = onNavigateToDownloadSettings,
        onOpenNetworkSettings = onNavigateToNetworkSettings,
        onOpenAppLanguageSettings = { value ->
            val old = Preferences.appLanguage
            if (old != value) {
                Preferences.appLanguage = value
                refreshKey++
                actions.applyAppLanguage()
            }
        },
        onCheckUpdate = {
            val currentVersion = versionState
            if (currentVersion is WebsiteState.Success && currentVersion.info != null) {
                actions.showPendingUpdateDialog()
            } else {
                actions.checkUpdate(forceShow = true)
            }
        },
        onUpdatePopupIntervalDaysChange = {
            Preferences.updatePopupIntervalDays = it
            refreshKey++
        },
        onOpenApplyDeepLinks = {
            if (!actions.openDeepLinkSettings()) {
                toaster.showShort(deepLinksNotSupported)
            }
        },
        onOpenFakeLauncherIcon = { showLauncherPicker = true },
        onOpenOpenSourceLicense = { showLicenseScreen = true },
        onOpenAbout = {},
        onClearCache = {
            if (cacheSize == 0L) {
                toaster.showShort(cacheEmpty)
                return@HomeSettingsScreen
            }
            showClearCacheConfirm = true
        },
        onExportBackup = {
            exportLauncher.launch(
                suggestedName = "Han1meViewer-backup-${Clock.System.now().toEpochMilliseconds()}",
                defaultExtension = "json",
            )
        },
        onImportBackup = {
            importLauncher.launch()
        },
        onSubmitBug = { uriHandler.openUri(HA1_GITHUB_ISSUE_URL) },
        onOpenForum = { uriHandler.openUri(HA1_GITHUB_FORUM_URL) },
    )

    ConfirmDialog(
        visible = pendingImportFile != null,
        title = stringResource(Res.string.backup_import_title),
        message = stringResource(Res.string.backup_import_confirm_message),
        confirmText = stringResource(Res.string.confirm),
        dismissText = stringResource(Res.string.cancel),
        onConfirm = {
            val file = pendingImportFile ?: return@ConfirmDialog
            pendingImportFile = null
            coroutineScope.launch {
                runCatching { BackupManager.importFrom(file) }
                    .onSuccess {
                        toaster.showShort(importSuccess)
                        refreshKey++
                        actions.reloadUi()
                    }
                    .onFailure { toaster.showShort(importFailed) }
            }
        },
        onDismiss = { pendingImportFile = null },
    )

    ConfirmDialog(
        visible = showClearCacheConfirm,
        title = stringResource(Res.string.sure_to_clear),
        message = stringResource(Res.string.sure_to_clear_cache),
        confirmText = stringResource(Res.string.confirm),
        dismissText = stringResource(Res.string.cancel),
        onConfirm = {
            showClearCacheConfirm = false
            coroutineScope.launch {
                val success = clearAppCache()
                cacheKey++
                refreshKey++
                if (success) toaster.showShort(clearSuccess) else toaster.showShort(clearFailed)
            }
        },
        onDismiss = { showClearCacheConfirm = false },
    )

    if (showLicenseScreen) {
        LicenseDialog(
            onDismiss = { showLicenseScreen = false }
        )
    }

    if (showAnalyticsDialog) {
        AnalyticsConsentDialog(
            onAccept = { showAnalyticsDialog = false },
            onDeny = {
                Preferences.isAnalyticsEnabled = false
                refreshKey++
                actions.setAnalyticsEnabled(false)
                showAnalyticsDialog = false
            },
        )
    }

    ConfirmDialog(
        visible = showRestartConfirmDialog,
        title = stringResource(Res.string.attention),
        message = stringResource(Res.string.restart_needed),
        confirmText = stringResource(Res.string.confirm),
        dismissText = stringResource(Res.string.cancel),
        cancelable = false,
        onConfirm = {
            actions.restartApp()
        },
        onDismiss = { showRestartConfirmDialog = false },
    )

    if (showLauncherPicker) {
        FakeLauncherIconDialog(
            onSelect = { option ->
                Preferences.fakeLauncherIcon = option.alias
                actions.switchLauncherIcon(option.alias)
                toaster.showShort(fakeIconHint)
                refreshKey++
                showLauncherPicker = false
            },
            onDismiss = { showLauncherPicker = false },
        )
    }
}


/**
 * @param refreshKey 只用来触发重算——`Preferences` 不是可观察状态，
 *   改完得靠它把这个 composable 拉一遍。
 */
@Composable
private fun buildHomeSettingsUiState(
    launcherOptions: List<LauncherIconOption>,
    updateSummary: String,
    cacheSummary: String,
    themeColorName: String,
    refreshKey: Int,
): HomeSettingsUiState {
    val currentAlias = Preferences.fakeLauncherIcon
    val currentOption = launcherOptions.find { it.alias == currentAlias } ?: launcherOptions.first()
    val currentOptionName = stringResource(currentOption.name)
    val videoLanguageLabel = when (Preferences.videoLanguage) {
        "zht" -> stringResource(Res.string.traditional_chinese)
        "zhs" -> stringResource(Res.string.simplified_chinese)
        else -> Preferences.videoLanguage
    }
    val darkModeLabel = when (Preferences.useDarkMode) {
        "follow_system" -> stringResource(Res.string.follow_system)
        "always_off" -> stringResource(Res.string.always_off)
        "always_on" -> stringResource(Res.string.always_on)
        else -> Preferences.useDarkMode
    }
    val appLanguageValue = Preferences.appLanguage
    val appLanguageLabel = when (appLanguageValue) {
        "system" -> stringResource(Res.string.follow_system)
        "zh-rCN" -> stringResource(Res.string.simplified_chinese)
        "zh" -> stringResource(Res.string.traditional_chinese)
        "ja" -> stringResource(Res.string.japanese_lang)
        "en" -> stringResource(Res.string.english_lang)
        else -> appLanguageValue
    }
    val searchGridColumnsConfig = Preferences.searchGridColumnsConfig
    val horizontalCardCountConfig = Preferences.horizontalCardCountConfig
    return HomeSettingsUiState(
        videoLanguage = Preferences.videoLanguage,
        videoLanguageLabel = videoLanguageLabel,
        defaultVideoQuality = Preferences.videoQuality,
        darkMode = Preferences.useDarkMode,
        darkModeLabel = darkModeLabel,
        appLanguage = appLanguageValue,
        appLanguageLabel = appLanguageLabel,
        allowPipMode = Preferences.allowPipMode,
        allowResumePlayback = Preferences.allowResumePlayback,
        showPlayedIndicator = Preferences.showPlayedIndicator,
        searchArtistIgnoreVideoType = Preferences.searchArtistIgnoreVideoType,
        disableMobileDataWarning = Preferences.disableMobileDataWarning,
        disablePredictiveBack = Preferences.disablePredictiveBack,
        tabletMode = Preferences.tabletMode,
        disableComments = Preferences.disableComments,
        collapseDownloadedGroup = Preferences.collapseDownloadedGroup,
        useDynamicColor = Preferences.useDynamicColor,
        useCIUpdateChannel = Preferences.useCIUpdateChannel,
        useAnalytics = Preferences.isAnalyticsEnabled,
        useLockScreen = Preferences.useLockScreen,
        fakeLauncherIconName = currentOptionName,
        updateSummary = updateSummary,
        cacheSummary = cacheSummary,
        versionSummary = stringResource(
            Res.string.current_version,
            "v${BuildConfig.VERSION_NAME}",
        ),
        updatePopupIntervalSummary = toIntervalDaysPrettyString(
            Preferences.updatePopupIntervalDays
        ),
        updatePopupIntervalDays = Preferences.updatePopupIntervalDays,
        themeColorKey = Preferences.themeColor ?: ThemeColorPreset.DEFAULT.key,
        themeColorName = themeColorName,
        searchGridColumnsSummary = listOf(
            searchGridColumnsConfig.compactColumns,
            searchGridColumnsConfig.mediumColumns,
            searchGridColumnsConfig.expandedColumns,
            searchGridColumnsConfig.largeColumns,
        ).joinToString(" / "),
        searchGridColumnsConfig = searchGridColumnsConfig,
        horizontalCardCountSummary = listOf(
            horizontalCardCountConfig.narrowCount,
            horizontalCardCountConfig.compactCount,
            horizontalCardCountConfig.mediumCount,
            horizontalCardCountConfig.expandedCount,
        ).joinToString(" / "),
        horizontalCardCountConfig = horizontalCardCountConfig,
        homeCategoryItems = defaultHomeCategoryPreferenceItems,
        homeCategoryOrder = homeCategoryOrder,
        hiddenHomeCategoryKeys = hiddenHomeCategoryKeys,
        useAvHomeCategoryTitles = Preferences.baseUrl == HanimeConstants.HANIME_URL[3],
    )
}
