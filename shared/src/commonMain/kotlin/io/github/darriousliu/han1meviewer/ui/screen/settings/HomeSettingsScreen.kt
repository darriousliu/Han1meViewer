package io.github.darriousliu.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.darriousliu.han1meviewer.HorizontalCardCountConfig
import io.github.darriousliu.han1meviewer.SearchGridColumnsConfig
import io.github.darriousliu.han1meviewer.ui.component.ChoiceDialog
import io.github.darriousliu.han1meviewer.ui.component.SettingInfoItem
import io.github.darriousliu.han1meviewer.ui.component.SettingNavigationItem
import io.github.darriousliu.han1meviewer.ui.component.SettingSliderItem
import io.github.darriousliu.han1meviewer.ui.component.SettingSwitchItem
import io.github.darriousliu.han1meviewer.ui.component.lazy.LazyColumn
import io.github.darriousliu.han1meviewer.ui.preview.ComponentPreview
import io.github.darriousliu.han1meviewer.ui.screen.settings.dialog.HomeCategoryLayoutDialog
import io.github.darriousliu.han1meviewer.ui.screen.settings.dialog.HorizontalCardCountDialog
import io.github.darriousliu.han1meviewer.ui.screen.settings.dialog.SearchGridColumnsDialog
import io.github.darriousliu.han1meviewer.ui.screen.settings.model.HomeSettingsCapabilities
import io.github.darriousliu.han1meviewer.ui.screen.settings.model.HomeSettingsUiState
import io.github.darriousliu.han1meviewer.ui.screen.settings.model.homeSettingsCapabilities
import io.github.darriousliu.han1meviewer.ui.theme.ThemeColorPreset
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.about
import io.github.darriousliu.han1meviewer.core.resource.allow_pip_disc
import io.github.darriousliu.han1meviewer.core.resource.allow_pip_title
import io.github.darriousliu.han1meviewer.core.resource.always_off
import io.github.darriousliu.han1meviewer.core.resource.always_on
import io.github.darriousliu.han1meviewer.core.resource.analytics_summary
import io.github.darriousliu.han1meviewer.core.resource.analytics_title
import io.github.darriousliu.han1meviewer.core.resource.app_lang
import io.github.darriousliu.han1meviewer.core.resource.app_lang_sum
import io.github.darriousliu.han1meviewer.core.resource.apply_deep_links
import io.github.darriousliu.han1meviewer.core.resource.apply_deep_links_summary
import io.github.darriousliu.han1meviewer.core.resource.backup_export_summary
import io.github.darriousliu.han1meviewer.core.resource.backup_export_title
import io.github.darriousliu.han1meviewer.core.resource.backup_import_summary
import io.github.darriousliu.han1meviewer.core.resource.backup_import_title
import io.github.darriousliu.han1meviewer.core.resource.baseline_add_link_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_backup_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_bug_report_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_data_usage_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_forum_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_grid_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_h_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_mobile_data_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_prohibit_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_restore_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_row_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_simp_to_trad_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_sort_24
import io.github.darriousliu.han1meviewer.core.resource.check_update
import io.github.darriousliu.han1meviewer.core.resource.clear_cache
import io.github.darriousliu.han1meviewer.core.resource.collapse_downloaded_groups
import io.github.darriousliu.han1meviewer.core.resource.collapse_downloaded_groups_summary
import io.github.darriousliu.han1meviewer.core.resource.dark_theme
import io.github.darriousliu.han1meviewer.core.resource.default_video_quilty
import io.github.darriousliu.han1meviewer.core.resource.disable_comments_sum
import io.github.darriousliu.han1meviewer.core.resource.disable_comments_title
import io.github.darriousliu.han1meviewer.core.resource.disable_mobile_data_warning
import io.github.darriousliu.han1meviewer.core.resource.disable_mobile_data_warning_summary
import io.github.darriousliu.han1meviewer.core.resource.disable_predictive_back_title
import io.github.darriousliu.han1meviewer.core.resource.download
import io.github.darriousliu.han1meviewer.core.resource.download_settings
import io.github.darriousliu.han1meviewer.core.resource.english_lang
import io.github.darriousliu.han1meviewer.core.resource.fake_app_icon
import io.github.darriousliu.han1meviewer.core.resource.follow_system
import io.github.darriousliu.han1meviewer.core.resource.forum
import io.github.darriousliu.han1meviewer.core.resource.forum_summary
import io.github.darriousliu.han1meviewer.core.resource.h_keyframe_settings
import io.github.darriousliu.han1meviewer.core.resource.home_category_layout
import io.github.darriousliu.han1meviewer.core.resource.home_category_layout_summary
import io.github.darriousliu.han1meviewer.core.resource.horizontal_card_count_summary
import io.github.darriousliu.han1meviewer.core.resource.horizontal_card_count_title
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_arrow_back_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_clear_all_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_download_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_fold_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_history_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_info_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_language_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_mask
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_moon_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_play_circle_outline_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_skip_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_tablet_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_theme_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_update_24
import io.github.darriousliu.han1meviewer.core.resource.ic_clock
import io.github.darriousliu.han1meviewer.core.resource.ic_comments
import io.github.darriousliu.han1meviewer.core.resource.ic_oss
import io.github.darriousliu.han1meviewer.core.resource.ic_pip_mode
import io.github.darriousliu.han1meviewer.core.resource.ic_setting_applock
import io.github.darriousliu.han1meviewer.core.resource.ic_setting_ci
import io.github.darriousliu.han1meviewer.core.resource.ic_setting_lang
import io.github.darriousliu.han1meviewer.core.resource.ic_video_quilty
import io.github.darriousliu.han1meviewer.core.resource.japanese_lang
import io.github.darriousliu.han1meviewer.core.resource.network
import io.github.darriousliu.han1meviewer.core.resource.network_settings
import io.github.darriousliu.han1meviewer.core.resource.open_source_license
import io.github.darriousliu.han1meviewer.core.resource.other
import io.github.darriousliu.han1meviewer.core.resource.player_settings
import io.github.darriousliu.han1meviewer.core.resource.privacy
import io.github.darriousliu.han1meviewer.core.resource.resume_playback_summary
import io.github.darriousliu.han1meviewer.core.resource.resume_playback_title
import io.github.darriousliu.han1meviewer.core.resource.search_artist_ignore_video_type
import io.github.darriousliu.han1meviewer.core.resource.search_artist_ignore_video_type_summary
import io.github.darriousliu.han1meviewer.core.resource.search_grid_columns_summary
import io.github.darriousliu.han1meviewer.core.resource.search_grid_columns_title
import io.github.darriousliu.han1meviewer.core.resource.select_fake_icon
import io.github.darriousliu.han1meviewer.core.resource.show_played_indicator
import io.github.darriousliu.han1meviewer.core.resource.show_played_indicator_summary
import io.github.darriousliu.han1meviewer.core.resource.simplified_chinese
import io.github.darriousliu.han1meviewer.core.resource.submit_bug
import io.github.darriousliu.han1meviewer.core.resource.submit_bug_summary
import io.github.darriousliu.han1meviewer.core.resource.tablet_mode
import io.github.darriousliu.han1meviewer.core.resource.tablet_mode_summary
import io.github.darriousliu.han1meviewer.core.resource.theme
import io.github.darriousliu.han1meviewer.core.resource.theme_color
import io.github.darriousliu.han1meviewer.core.resource.traditional_chinese
import io.github.darriousliu.han1meviewer.core.resource.update
import io.github.darriousliu.han1meviewer.core.resource.update_popup_interval_days
import io.github.darriousliu.han1meviewer.core.resource.use_ci_update_channel
import io.github.darriousliu.han1meviewer.core.resource.use_lock_screen
import io.github.darriousliu.han1meviewer.core.resource.use_lock_screen_sum
import io.github.darriousliu.han1meviewer.core.resource.video
import io.github.darriousliu.han1meviewer.core.resource.video_language
import org.jetbrains.compose.resources.stringResource

private enum class HomeSettingsChoiceDialog {
    VideoLanguage,
    VideoQuality,
    DarkMode,
    AppLanguage,
    ThemeColor,
}

@Composable
fun HomeSettingsScreen(
    state: HomeSettingsUiState,
    capabilities: HomeSettingsCapabilities = homeSettingsCapabilities,
    onVideoLanguageChange: (String) -> Unit,
    onVideoQualityChange: (String) -> Unit,
    onDarkModeChange: (String) -> Unit,
    onAllowPipModeChange: (Boolean) -> Unit,
    onAllowResumePlaybackChange: (Boolean) -> Unit,
    onShowPlayedIndicatorChange: (Boolean) -> Unit,
    onSearchArtistIgnoreVideoTypeChange: (Boolean) -> Unit,
    onDisableMobileDataWarningChange: (Boolean) -> Unit,
    onDisablePredictiveBackChange: (Boolean) -> Unit,
    onTabletModeChange: (Boolean) -> Unit,
    onDisableCommentsChange: (Boolean) -> Unit,
    onCollapseDownloadedGroupChange: (Boolean) -> Unit,
    onSearchGridColumnsConfigChange: (SearchGridColumnsConfig) -> Unit,
    onHorizontalCardCountConfigChange: (HorizontalCardCountConfig) -> Unit,
    onUseCIUpdateChannelChange: (Boolean) -> Unit,
    onUseAnalyticsChange: (Boolean) -> Unit,
    onUseLockScreenChange: (Boolean) -> Unit,
    onThemeColorChange: (String) -> Unit,
    onHomeCategoryPreferencesChange: (List<String>, Set<String>) -> Unit,
    onOpenPlayerSettings: () -> Unit,
    onOpenHKeyframeSettings: () -> Unit,
    onOpenDownloadSettings: () -> Unit,
    onOpenNetworkSettings: () -> Unit,
    onOpenAppLanguageSettings: (String) -> Unit,
    onCheckUpdate: () -> Unit,
    onUpdatePopupIntervalDaysChange: (Int) -> Unit,
    onOpenApplyDeepLinks: () -> Unit,
    onOpenFakeLauncherIcon: () -> Unit,
    onOpenOpenSourceLicense: () -> Unit,
    onOpenAbout: () -> Unit,
    onClearCache: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onSubmitBug: () -> Unit,
    onOpenForum: () -> Unit,
) {
    var activeDialog by rememberSaveable { mutableStateOf<HomeSettingsChoiceDialog?>(null) }
    var showSearchGridColumnsDialog by rememberSaveable { mutableStateOf(false) }
    var showHorizontalCardCountDialog by rememberSaveable { mutableStateOf(false) }
    var showHomeCategoryDialog by rememberSaveable { mutableStateOf(false) }

    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.VideoLanguage,
        title = stringResource(Res.string.video_language),
        options = listOf(
            stringResource(Res.string.traditional_chinese) to "zht",
            stringResource(Res.string.simplified_chinese) to "zhs",
        ),
        selectedValue = state.videoLanguage,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onVideoLanguageChange(it)
        },
    )

    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.VideoQuality,
        title = stringResource(Res.string.default_video_quilty),
        options = listOf("480P" to "480P", "720P" to "720P", "1080P" to "1080P"),
        selectedValue = state.defaultVideoQuality,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onVideoQualityChange(it)
        },
    )

    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.DarkMode,
        title = stringResource(Res.string.dark_theme),
        options = listOf(
            stringResource(Res.string.follow_system) to "follow_system",
            stringResource(Res.string.always_off) to "always_off",
            stringResource(Res.string.always_on) to "always_on",
        ),
        selectedValue = state.darkMode,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onDarkModeChange(it)
        },
    )

    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.AppLanguage,
        title = stringResource(Res.string.app_lang),
        options = listOf(
            stringResource(Res.string.follow_system) to "system",
            stringResource(Res.string.simplified_chinese) to "zh-rCN",
            stringResource(Res.string.traditional_chinese) to "zh",
            stringResource(Res.string.japanese_lang) to "ja",
            stringResource(Res.string.english_lang) to "en",
        ),
        selectedValue = state.appLanguage,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onOpenAppLanguageSettings(it)
        },
    )

    ChoiceDialog(
        visible = activeDialog == HomeSettingsChoiceDialog.ThemeColor,
        title = stringResource(Res.string.theme_color),
        options = ThemeColorPreset.entries
            .filter { capabilities.dynamicColor || it != ThemeColorPreset.SYSTEM }
            .map { stringResource(it.displayNameRes) to it.key },
        selectedValue = state.themeColorKey,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onThemeColorChange(it)
        },
    )

    if (showSearchGridColumnsDialog) {
        SearchGridColumnsDialog(
            initialConfig = state.searchGridColumnsConfig,
            onDismiss = { showSearchGridColumnsDialog = false },
            onConfirm = {
                showSearchGridColumnsDialog = false
                onSearchGridColumnsConfigChange(it)
            },
        )
    }

    if (showHorizontalCardCountDialog) {
        HorizontalCardCountDialog(
            initialConfig = state.horizontalCardCountConfig,
            onDismiss = { showHorizontalCardCountDialog = false },
            onConfirm = {
                showHorizontalCardCountDialog = false
                onHorizontalCardCountConfigChange(it)
            },
        )
    }

    if (showHomeCategoryDialog) {
        HomeCategoryLayoutDialog(
            state = state,
            onDismiss = { showHomeCategoryDialog = false },
            onConfirm = { order, hiddenKeys ->
                showHomeCategoryDialog = false
                onHomeCategoryPreferencesChange(order, hiddenKeys)
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item { SettingsGroupTitle(stringResource(Res.string.video)) }
        item {
            SettingNavigationItem(
                title = stringResource(Res.string.video_language),
                valueText = state.videoLanguageLabel,
                iconRes = Res.drawable.baseline_simp_to_trad_24,
                onClick = { activeDialog = HomeSettingsChoiceDialog.VideoLanguage },
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(Res.string.default_video_quilty),
                valueText = state.defaultVideoQuality,
                iconRes = Res.drawable.ic_video_quilty,
                onClick = { activeDialog = HomeSettingsChoiceDialog.VideoQuality },
            )
        }
        if (capabilities.pictureInPicture) {
            item {
                SettingSwitchItem(
                    title = stringResource(Res.string.allow_pip_title),
                    summary = stringResource(Res.string.allow_pip_disc),
                    checked = state.allowPipMode,
                    iconRes = Res.drawable.ic_pip_mode,
                    onCheckedChange = onAllowPipModeChange,
                )
            }
        }
        item {
            SettingSwitchItem(
                title = stringResource(Res.string.resume_playback_title),
                summary = stringResource(Res.string.resume_playback_summary),
                checked = state.allowResumePlayback,
                iconRes = Res.drawable.ic_baseline_skip_24,
                onCheckedChange = onAllowResumePlaybackChange,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(Res.string.show_played_indicator),
                summary = stringResource(Res.string.show_played_indicator_summary),
                checked = state.showPlayedIndicator,
                iconRes = Res.drawable.ic_baseline_history_24,
                onCheckedChange = onShowPlayedIndicatorChange,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(Res.string.horizontal_card_count_title),
                summary = stringResource(Res.string.horizontal_card_count_summary),
                valueText = state.horizontalCardCountSummary,
                iconRes = Res.drawable.baseline_row_24,
                onClick = { showHorizontalCardCountDialog = true },
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(Res.string.search_artist_ignore_video_type),
                summary = stringResource(Res.string.search_artist_ignore_video_type_summary),
                checked = state.searchArtistIgnoreVideoType,
                iconRes = Res.drawable.baseline_prohibit_24,
                onCheckedChange = onSearchArtistIgnoreVideoTypeChange,
            )
        }
        if (capabilities.mobileDataWarning) {
            item {
                SettingSwitchItem(
                    title = stringResource(Res.string.disable_mobile_data_warning),
                    summary = stringResource(Res.string.disable_mobile_data_warning_summary),
                    checked = state.disableMobileDataWarning,
                    iconRes = Res.drawable.baseline_mobile_data_24,
                    onCheckedChange = onDisableMobileDataWarningChange,
                )
            }
        }
        if (capabilities.predictiveBack) {
            item {
                SettingSwitchItem(
                    title = stringResource(Res.string.disable_predictive_back_title),
                    summary = "暂不可用 Temporarily unavailable",
                    checked = state.disablePredictiveBack,
                    iconRes = Res.drawable.ic_baseline_arrow_back_24,
                    onCheckedChange = onDisablePredictiveBackChange,
                    enabled = false
                )
            }
        }
        item {
            SettingSwitchItem(
                title = stringResource(Res.string.tablet_mode),
                summary = stringResource(Res.string.tablet_mode_summary),
                checked = state.tabletMode,
                iconRes = Res.drawable.ic_baseline_tablet_24,
                onCheckedChange = onTabletModeChange,
            )
        }
        if (state.tabletMode) {
            item {
                SettingNavigationItem(
                    title = stringResource(Res.string.search_grid_columns_title),
                    summary = stringResource(Res.string.search_grid_columns_summary),
                    valueText = state.searchGridColumnsSummary,
                    iconRes = Res.drawable.baseline_grid_24,
                    onClick = { showSearchGridColumnsDialog = true },
                )
            }
        }
        item {
            SettingNavigationItem(
                title = stringResource(Res.string.player_settings),
                iconRes = Res.drawable.ic_baseline_play_circle_outline_24,
                onClick = onOpenPlayerSettings,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(Res.string.h_keyframe_settings),
                iconRes = Res.drawable.baseline_h_24,
                onClick = onOpenHKeyframeSettings,
            )
        }
        item {
            SettingSwitchItem(
                title = stringResource(Res.string.disable_comments_title),
                summary = stringResource(Res.string.disable_comments_sum),
                checked = state.disableComments,
                iconRes = Res.drawable.ic_comments,
                onCheckedChange = onDisableCommentsChange,
            )
        }

        if (capabilities.downloads) {
            item { SettingsGroupTitle(stringResource(Res.string.download)) }
            item {
                SettingNavigationItem(
                    title = stringResource(Res.string.download_settings),
                    iconRes = Res.drawable.ic_baseline_download_24,
                    onClick = onOpenDownloadSettings,
                )
            }
            item {
                SettingSwitchItem(
                    title = stringResource(Res.string.collapse_downloaded_groups),
                    summary = stringResource(Res.string.collapse_downloaded_groups_summary),
                    checked = state.collapseDownloadedGroup,
                    iconRes = Res.drawable.ic_baseline_fold_24,
                    onCheckedChange = onCollapseDownloadedGroupChange,
                )
            }
        }

        item { SettingsGroupTitle(stringResource(Res.string.network)) }
        item {
            SettingNavigationItem(
                title = stringResource(Res.string.network_settings),
                iconRes = Res.drawable.ic_baseline_language_24,
                onClick = onOpenNetworkSettings,
            )
        }
        if (capabilities.deepLinkSettings) {
            item {
                SettingNavigationItem(
                    title = stringResource(Res.string.apply_deep_links),
                    summary = stringResource(Res.string.apply_deep_links_summary),
                    iconRes = Res.drawable.baseline_add_link_24,
                    onClick = onOpenApplyDeepLinks,
                )
            }
        }

        item { SettingsGroupTitle(stringResource(Res.string.theme)) }
        if (capabilities.darkModeOverride) {
            item {
                SettingNavigationItem(
                    title = stringResource(Res.string.dark_theme),
                    valueText = state.darkModeLabel,
                    iconRes = Res.drawable.ic_baseline_moon_24,
                    onClick = { activeDialog = HomeSettingsChoiceDialog.DarkMode },
                )
            }
        }
        item {
            SettingNavigationItem(
                title = stringResource(Res.string.theme_color),
                valueText = state.themeColorName,
                iconRes = Res.drawable.ic_baseline_theme_24,
                onClick = { activeDialog = HomeSettingsChoiceDialog.ThemeColor },
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(Res.string.home_category_layout),
                summary = stringResource(
                    Res.string.home_category_layout_summary,
                    state.homeCategoryItems.size - state.hiddenHomeCategoryKeys.size,
                    state.homeCategoryItems.size,
                ),
                iconRes = Res.drawable.baseline_sort_24,
                onClick = { showHomeCategoryDialog = true },
            )
        }
        if (capabilities.appLanguageOverride) {
            item {
                SettingNavigationItem(
                    title = stringResource(Res.string.app_lang),
                    summary = stringResource(Res.string.app_lang_sum),
                    valueText = state.appLanguageLabel,
                    iconRes = Res.drawable.ic_setting_lang,
                    onClick = { activeDialog = HomeSettingsChoiceDialog.AppLanguage },
                )
            }
        }

        if (capabilities.updateCheck || capabilities.ciUpdateChannel) {
            item { SettingsGroupTitle(stringResource(Res.string.update)) }
        }
        if (capabilities.updateCheck) {
            item {
                SettingNavigationItem(
                    title = stringResource(Res.string.check_update),
                    summary = state.updateSummary,
                    iconRes = Res.drawable.ic_baseline_update_24,
                    onClick = onCheckUpdate,
                )
            }
        }
        if (capabilities.ciUpdateChannel) {
            item {
                SettingSwitchItem(
                    title = stringResource(Res.string.use_ci_update_channel),
                    checked = state.useCIUpdateChannel,
                    iconRes = Res.drawable.ic_setting_ci,
                    onCheckedChange = onUseCIUpdateChannelChange,
                )
            }
        }
        if (capabilities.updateCheck) {
            item {
                SettingSliderItem(
                    title = stringResource(Res.string.update_popup_interval_days),
                    summary = state.updatePopupIntervalSummary,
                    value = state.updatePopupIntervalDays,
                    valueRange = 0..30,
                    iconRes = Res.drawable.ic_clock,
                    onValueChange = onUpdatePopupIntervalDaysChange,
                )
            }
        }

        if (capabilities.analytics || capabilities.appLock || capabilities.fakeLauncherIcon) {
            item { SettingsGroupTitle(stringResource(Res.string.privacy)) }
        }
        if (capabilities.analytics) {
            item {
                SettingSwitchItem(
                    title = stringResource(Res.string.analytics_title),
                    summary = stringResource(Res.string.analytics_summary),
                    checked = state.useAnalytics,
                    iconRes = Res.drawable.baseline_data_usage_24,
                    onCheckedChange = onUseAnalyticsChange,
                )
            }
        }
        if (capabilities.appLock) {
            item {
                SettingSwitchItem(
                    title = stringResource(Res.string.use_lock_screen),
                    summary = stringResource(Res.string.use_lock_screen_sum),
                    checked = state.useLockScreen,
                    iconRes = Res.drawable.ic_setting_applock,
                    onCheckedChange = onUseLockScreenChange,
                )
            }
        }
        if (capabilities.fakeLauncherIcon) {
            item {
                SettingNavigationItem(
                    title = stringResource(Res.string.fake_app_icon),
                    summary = stringResource(Res.string.select_fake_icon),
                    valueText = state.fakeLauncherIconName,
                    iconRes = Res.drawable.ic_baseline_mask,
                    onClick = onOpenFakeLauncherIcon,
                )
            }
        }

        item { SettingsGroupTitle(stringResource(Res.string.other)) }
        item {
            SettingNavigationItem(
                title = stringResource(Res.string.backup_export_title),
                summary = stringResource(Res.string.backup_export_summary),
                iconRes = Res.drawable.baseline_backup_24,
                onClick = onExportBackup,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(Res.string.backup_import_title),
                summary = stringResource(Res.string.backup_import_summary),
                iconRes = Res.drawable.baseline_restore_24,
                onClick = onImportBackup,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(Res.string.clear_cache),
                summary = state.cacheSummary,
                iconRes = Res.drawable.ic_baseline_clear_all_24,
                onClick = onClearCache,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(Res.string.submit_bug),
                summary = stringResource(Res.string.submit_bug_summary),
                iconRes = Res.drawable.baseline_bug_report_24,
                onClick = onSubmitBug,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(Res.string.forum),
                summary = stringResource(Res.string.forum_summary),
                iconRes = Res.drawable.baseline_forum_24,
                onClick = onOpenForum,
            )
        }
        item {
            SettingNavigationItem(
                title = stringResource(Res.string.open_source_license),
                iconRes = Res.drawable.ic_oss,
                onClick = onOpenOpenSourceLicense,
            )
        }
        item {
            SettingInfoItem(
                title = stringResource(Res.string.about),
                summary = state.versionSummary,
                iconRes = Res.drawable.ic_baseline_info_24,
            )
        }
    }
}

@Composable
private fun SettingsGroupTitle(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        HorizontalDivider()
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 1000)
@Composable
private fun HomeSettingsScreenPreview() = HomeSettingsScreenPreviewBody(homeSettingsCapabilities)

/** 全 false，等于 desktop/iOS 现在的样子——用来一眼看出门控掉之后还剩什么。 */
@Preview(showBackground = true, widthDp = 420, heightDp = 1000)
@Composable
private fun HomeSettingsScreenNoCapabilitiesPreview() =
    HomeSettingsScreenPreviewBody(HomeSettingsCapabilities())

@Composable
private fun HomeSettingsScreenPreviewBody(capabilities: HomeSettingsCapabilities) {
    ComponentPreview {
        HomeSettingsScreen(
            capabilities = capabilities,
            state = HomeSettingsUiState(
                videoLanguage = "zhs",
                videoLanguageLabel = "简体中文",
                defaultVideoQuality = "1080P",
                darkMode = "follow_system",
                darkModeLabel = "跟随系统",
                appLanguage = "system",
                appLanguageLabel = "跟随系统",
                allowPipMode = true,
                allowResumePlayback = true,
                showPlayedIndicator = true,
                searchArtistIgnoreVideoType = false,
                disableMobileDataWarning = false,
                disablePredictiveBack = false,
                tabletMode = false,
                disableComments = false,
                collapseDownloadedGroup = false,
                useDynamicColor = true,
                useCIUpdateChannel = false,
                useAnalytics = true,
                useLockScreen = false,
                fakeLauncherIconName = "Han1meViewer",
                updateSummary = "已經是最新版本！",
                cacheSummary = "目前佔用了 12MB 的儲存空間",
                versionSummary = "當前版本：v1.0.0",
                updatePopupIntervalSummary = "7天\n最近還沒跳出過更新視窗哦",
                updatePopupIntervalDays = 7,
                themeColorKey = "default",
                themeColorName = "預設（暖紅）",
                searchGridColumnsSummary = "2 / 3 / 4 / 5",
                searchGridColumnsConfig = SearchGridColumnsConfig(),
                horizontalCardCountSummary = "1.5 / 2.1 / 4.1 / 5.1",
                horizontalCardCountConfig = HorizontalCardCountConfig(),
                homeCategoryItems = emptyList(),
                homeCategoryOrder = emptyList(),
                hiddenHomeCategoryKeys = emptySet(),
                useAvHomeCategoryTitles = false,
            ),
            onVideoLanguageChange = {},
            onVideoQualityChange = {},
            onDarkModeChange = {},
            onAllowPipModeChange = {},
            onAllowResumePlaybackChange = {},
            onShowPlayedIndicatorChange = {},
            onSearchArtistIgnoreVideoTypeChange = {},
            onDisableMobileDataWarningChange = {},
            onDisablePredictiveBackChange = {},
            onTabletModeChange = {},
            onDisableCommentsChange = {},
            onCollapseDownloadedGroupChange = {},
            onSearchGridColumnsConfigChange = {},
            onHorizontalCardCountConfigChange = {},
            onUseCIUpdateChannelChange = {},
            onUseAnalyticsChange = {},
            onUseLockScreenChange = {},
            onThemeColorChange = {},
            onHomeCategoryPreferencesChange = { _, _ -> },
            onOpenPlayerSettings = {},
            onOpenHKeyframeSettings = {},
            onOpenDownloadSettings = {},
            onOpenNetworkSettings = {},
            onOpenAppLanguageSettings = {},
            onCheckUpdate = {},
            onUpdatePopupIntervalDaysChange = {},
            onOpenApplyDeepLinks = {},
            onOpenFakeLauncherIcon = {},
            onOpenOpenSourceLicense = {},
            onOpenAbout = {},
            onClearCache = {},
            onExportBackup = {},
            onImportBackup = {},
            onSubmitBug = {},
            onOpenForum = {},
        )
    }
}
