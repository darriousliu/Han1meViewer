package com.yenaly.han1meviewer.ui.navigation.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yenaly.han1meviewer.BuildConfig
import com.yenaly.han1meviewer.HA1_GITHUB_FORUM_URL
import com.yenaly.han1meviewer.HA1_GITHUB_ISSUE_URL
import com.yenaly.han1meviewer.HanimeConstants
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.BackupManager
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.screen.home.homepage.defaultHomeCategoryPreferenceItems
import com.yenaly.han1meviewer.ui.screen.home.homepage.hiddenHomeCategoryKeys
import com.yenaly.han1meviewer.ui.screen.home.homepage.homeCategoryOrder
import com.yenaly.han1meviewer.ui.screen.home.homepage.saveHomeCategoryPreferences
import com.yenaly.han1meviewer.ui.screen.settings.HomeSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.dialog.AnalyticsConsentDialog
import com.yenaly.han1meviewer.ui.screen.settings.dialog.FakeLauncherIconDialog
import com.yenaly.han1meviewer.ui.screen.settings.dialog.LauncherIconOption
import com.yenaly.han1meviewer.ui.screen.settings.dialog.launcherIconOptions
import com.yenaly.han1meviewer.ui.screen.settings.dialog.LicenseDialog
import com.yenaly.han1meviewer.ui.screen.settings.model.HomeSettingsUiState
import com.yenaly.han1meviewer.ui.theme.ThemeColorPreset
import com.yenaly.han1meviewer.util.applicationContext
import com.yenaly.han1meviewer.util.browse
import com.yenaly.han1meviewer.util.showShortToast
import com.yenaly.han1meviewer.util.showToast
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSettingsRouteScreen(
    onNavigateToPlayerSettings: () -> Unit,
    onNavigateToHKeyframeSettings: () -> Unit,
    onNavigateToDownloadSettings: () -> Unit,
    onNavigateToNetworkSettings: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val actions = rememberHomeSettingsActions()
    val versionState by actions.versionFlow.collectAsStateWithLifecycle()
    var refreshKey by remember { mutableIntStateOf(0) }
    var cacheKey by remember { mutableIntStateOf(0) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showLicenseScreen by remember { mutableStateOf(false) }
    var showRestartConfirmDialog by remember { mutableStateOf(false) }
    var showAnalyticsDialog by remember { mutableStateOf(false) }
    var showLauncherPicker by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch(Dispatchers.IO) {
            runCatching { BackupManager.exportTo(PlatformFile(uri)) }
                .onSuccess { withContext(Dispatchers.Main) { showShortToast(R.string.backup_export_success) } }
                .onFailure { withContext(Dispatchers.Main) { showShortToast(R.string.backup_export_failed) } }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingImportUri = uri
    }

    // 算目录大小是 IO，拼文案是资源——摘要文案现在是 @Composable（commonMain 的
    // SettingsSummaries），不能在协程里调，所以这里只留大小，文案在下面组合时拼
    var cacheSize by remember { mutableLongStateOf(0L) }

    LaunchedEffect(cacheKey) {
        cacheSize = actions.cacheSizeBytes()
    }
    val cacheSummary = generateClearCacheSummary(cacheSize)
    val checkUpdateFailed = stringResource(R.string.check_update_failed)
    val checkingUpdate = stringResource(R.string.checking_update)
    val alreadyLatestUpdate = stringResource(R.string.already_latest_update)

    val updateSummary = remember(versionState, context) {
        when (versionState) {
            is WebsiteState.Error -> checkUpdateFailed
            is WebsiteState.Loading -> checkingUpdate
            is WebsiteState.Success -> {
                val info = (versionState as WebsiteState.Success).info
                if (info == null) {
                    alreadyLatestUpdate
                } else {
                    applicationContext.getString(R.string.check_update_success, info.version)
                }
            }
        }
    }
    val themeColorName = stringResource(ThemeColorPreset.fromKey(Preferences.themeColor).displayNameRes)
    val uiState = buildHomeSettingsUiState(
        context = context,
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
            context.showToast(R.string.success_value, value)
        },
        onDarkModeChange = { value ->
            if (value != Preferences.useDarkMode) {
                Preferences.useDarkMode = value
                actions.applyDarkMode()
            }
        },
        onAllowPipModeChange = { enabled ->
            if (enabled && !actions.isPipPermissionGranted()) {
                context.showToast(R.string.request_pip_alert)
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
                        context.showToast(R.string.not_set_sys_lock)
                        refreshKey++
                        return@HomeSettingsScreen
                    }

                    DeviceLockAvailability.UnsupportedOsVersion,
                    DeviceLockAvailability.Unsupported -> {
                        context.showToast(R.string.not_compact_lock_screen)
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
                showShortToast(R.string.action_app_open_by_default_settings_not_support)
            }
        },
        onOpenFakeLauncherIcon = { showLauncherPicker = true },
        onOpenOpenSourceLicense = { showLicenseScreen = true },
        onOpenAbout = {},
        onClearCache = {
            if (cacheSize == 0L) {
                showShortToast(R.string.cache_empty)
                return@HomeSettingsScreen
            }
            showClearCacheConfirm = true
        },
        onExportBackup = {
            exportLauncher.launch("Han1meViewer-backup-${System.currentTimeMillis()}.json")
        },
        onImportBackup = {
            importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
        },
        onSubmitBug = { context.browse(HA1_GITHUB_ISSUE_URL) },
        onOpenForum = { context.browse(HA1_GITHUB_FORUM_URL) },
    )

    ConfirmDialog(
        visible = pendingImportUri != null,
        title = stringResource(R.string.backup_import_title),
        message = stringResource(R.string.backup_import_confirm_message),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            val uri = pendingImportUri ?: return@ConfirmDialog
            pendingImportUri = null
            coroutineScope.launch(Dispatchers.IO) {
                runCatching { BackupManager.importFrom(PlatformFile(uri)) }
                    .onSuccess {
                        withContext(Dispatchers.Main) {
                            showShortToast(R.string.backup_import_success)
                            refreshKey++
                            actions.reloadUi()
                        }
                    }
                    .onFailure {
                        withContext(Dispatchers.Main) {
                            showShortToast(R.string.backup_import_failed)
                        }
                    }
            }
        },
        onDismiss = { pendingImportUri = null },
    )

    ConfirmDialog(
        visible = showClearCacheConfirm,
        title = stringResource(R.string.sure_to_clear),
        message = stringResource(R.string.sure_to_clear_cache),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            showClearCacheConfirm = false
            coroutineScope.launch {
                val success = actions.clearCache()
                cacheKey++
                refreshKey++
                if (success) showShortToast(R.string.clear_success) else showShortToast(R.string.clear_failed)
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
        title = stringResource(R.string.attention),
        message = stringResource(R.string.restart_needed),
        confirmText = stringResource(R.string.confirm),
        dismissText = stringResource(R.string.cancel),
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
                context.showToast(R.string.fake_icon_hint)
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
    context: Context,
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
        "zht" -> context.getString(R.string.traditional_chinese)
        "zhs" -> context.getString(R.string.simplified_chinese)
        else -> Preferences.videoLanguage
    }
    val darkModeLabel = when (Preferences.useDarkMode) {
        "follow_system" -> context.getString(R.string.follow_system)
        "always_off" -> context.getString(R.string.always_off)
        "always_on" -> context.getString(R.string.always_on)
        else -> Preferences.useDarkMode
    }
    val appLanguageValue = Preferences.appLanguage
    val appLanguageLabel = when (appLanguageValue) {
        "system" -> context.getString(R.string.follow_system)
        "zh-rCN" -> context.getString(R.string.simplified_chinese)
        "zh" -> context.getString(R.string.traditional_chinese)
        "ja" -> context.getString(R.string.japanese_lang)
        "en" -> context.getString(R.string.english_lang)
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
        versionSummary = context.getString(
            R.string.current_version,
            "v${BuildConfig.VERSION_NAME}"
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
