package com.yenaly.han1meviewer.ui.navigation.settings

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.yenaly.han1meviewer.HanimeApplication
import com.yenaly.han1meviewer.logic.model.github.Latest
import com.yenaly.han1meviewer.logic.state.WebsiteState
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.util.ThemeUtils
import com.yenaly.han1meviewer.util.applyAppLanguage
import com.yenaly.han1meviewer.util.toLanguageTagOrNull
import com.yenaly.han1meviewer.util.activity
import com.yenaly.han1meviewer.util.restartApplication
import kotlinx.coroutines.flow.StateFlow

/**
 * `LocalActivity` 来自 activity-compose（项目在 1.13.0，远高于引入它的 1.10.0）。
 * 这个 route 永远 composed 在 `MainActivity.setContent` 里，所以拿得到；
 * 兜底再走一遍 `Context.activity`。
 */
@Composable
actual fun rememberHomeSettingsActions(): HomeSettingsActions {
    val context = LocalContext.current
    val activity = LocalActivity.current ?: context.activity
    return remember(context, activity) { AndroidHomeSettingsActions(context, activity) }
}

private class AndroidHomeSettingsActions(
    private val context: Context,
    private val activity: Activity?,
) : HomeSettingsActions {

    /**
     * Compose 主题现在直接观察 `Preferences.useDarkModeStateFlow`（见 `HanimeTheme`），
     * 所以**不再 `recreate()`**。
     *
     * `setDefaultNightMode` 仍然要调：XML 主题的 `values-night` 和系统栏图标配色
     * 还靠它，只是它不再负责驱动 Compose 那半。
     */
    override fun applyDarkMode() {
        ThemeUtils.applyDarkModeFromPreferences(context)
    }

    override fun applyThemeColor() = recreate()

    /**
     * 换成平台标准的 per-app language API（见 `util/AppLocale.android.kt`）。
     * 它自己会触发重建，所以这里**不再手动 `recreate()`**。
     */
    override fun applyAppLanguage() =
        applyAppLanguage(toLanguageTagOrNull(Preferences.appLanguage))

    override fun reloadUi() = recreate()

    override fun restartApp() = restartApplication(killProcess = true)

    override fun isPipPermissionGranted(): Boolean = isPipPermissionGranted(context)

    override fun openPipPermissionSettings() = openPipPermissionSettings(context)

    override fun deviceLockAvailability(): DeviceLockAvailability = when {
        !isDeviceSecureCompat(context) -> DeviceLockAvailability.NoSystemLock
        Build.VERSION.SDK_INT < Build.VERSION_CODES.P -> DeviceLockAvailability.UnsupportedOsVersion
        else -> DeviceLockAvailability.Available
    }

    override fun setAnalyticsEnabled(enabled: Boolean) {
        Firebase.analytics.setAnalyticsCollectionEnabled(enabled)
    }

    override fun openDeepLinkSettings(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        // 这个弹窗要真 Activity：它内层那个 Intent 没有 FLAG_ACTIVITY_NEW_TASK，
        // 用 applicationContext 起不来
        val activity = activity ?: run {
            logMissingActivity("openDeepLinkSettings")
            return false
        }
        showApplyDeepLinksDialog(context, activity)
        return true
    }

    override fun switchLauncherIcon(alias: String) {
        (context.applicationContext as? HanimeApplication)?.switchLauncher(alias)
    }

    override val versionFlow: StateFlow<WebsiteState<Latest?>>
        get() = AppViewModel.versionFlow

    override fun checkUpdate(forceShow: Boolean) {
        if (forceShow) AppViewModel.getLatestVersion(forceShow = true)
        else AppViewModel.getLatestVersion()
    }

    override fun showPendingUpdateDialog() = AppViewModel.showUpdateDialogIfAvailable()

    override fun onUpdateChannelChanged() = AppViewModel.getLatestVersion()

    private fun recreate() {
        val activity = activity ?: return logMissingActivity("recreate")
        activity.recreate()
    }

    /**
     * 原来是硬调 `activity.recreate()`，现在空安全了，多出一个以前没有的静默失败面。
     * 理论上走不到（route 永远在 `MainActivity.setContent` 里），走到了要能看见。
     */
    private fun logMissingActivity(action: String) {
        android.util.Log.w("HomeSettingsActions", "no Activity available for $action")
    }
}
