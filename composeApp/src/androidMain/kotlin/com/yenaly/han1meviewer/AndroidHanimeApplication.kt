package com.yenaly.han1meviewer

import android.content.ComponentName
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.color.DynamicColors
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.platform.AppBuildInfoProvider
import com.yenaly.han1meviewer.platform.FirebaseRuntimeConfiguration
import com.yenaly.han1meviewer.platform.firebasePlatform
import com.yenaly.han1meviewer.platform.platformServices
import com.yenaly.han1meviewer.storage.AndroidStorageBootstrap
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel
import com.yenaly.han1meviewer.util.AnimeShaders
import com.yenaly.han1meviewer.util.ThemeUtils
import com.yenaly.yenaly_libs.base.YenalyApplication
import java.net.ProxySelector

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/08 008 17:32
 */
open class AndroidHanimeApplication : YenalyApplication() {

    companion object {
        const val TAG = "HanimeApplication"
    }

    /**
     * 已经在 [HInitializer] 中处理了
     */
    override val isDefaultCrashHandlerEnabled: Boolean = false

    override fun onCreate() {
        super.onCreate()
        AndroidStorageBootstrap.ensureInitialized(this)
        ThemeUtils.applyDarkModeFromPreferences(this)
        if (Preferences.useDynamicColor){
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
        ProxySelector.setDefault(HProxySelector())
        HProxySelector.rebuildNetwork()
        initFirebase()
        initNotificationChannel()
        if (AnimeShaders.copyShaderAssets(applicationContext) <= 0) {
            Log.w(TAG, "Shader 复制失败")
        }
        if (AnimeShaders.copyCertAssets(applicationContext) <= 0) {
            Log.w(TAG, "cert 复制失败")
        }
        val selected = Preferences.fakeLauncherIcon
        switchLauncher(selected)
    }

    private fun initFirebase() {
        firebasePlatform().initialize(
            configuration = FirebaseRuntimeConfiguration(
                analyticsCollectionEnabled = Preferences.isAnalyticsEnabled,
                crashlyticsCollectionEnabled = !AppBuildInfoProvider.current.debug,
                crashlyticsStringKeys = mapOf(
                    FirebaseConstants.APP_LANGUAGE to
                        platformServices().language.preferredLanguage().tag,
                    FirebaseConstants.VERSION_SOURCE to
                        AppBuildInfoProvider.current.versionSource,
                ),
                remoteConfigMinimumFetchIntervalSeconds =
                    if (AppBuildInfoProvider.current.debug) 0 else 3 * 60 * 60,
                remoteConfigFetchTimeoutSeconds = 10,
                remoteConfigDefaults = FirebaseConstants.remoteConfigDefaults,
                realtimeDatabasePersistenceEnabled = true,
            ),
            onRemoteConfigActivated = {
                AppViewModel.getLatestVersion(delayMillis = 200)
            },
        )
    }

    private fun initNotificationChannel() {
        val nm = NotificationManagerCompat.from(this)

        val hanimeDownloadChannel = NotificationChannelCompat.Builder(
            DOWNLOAD_NOTIFICATION_CHANNEL,
            NotificationManagerCompat.IMPORTANCE_HIGH
        ).setName("Hanime Download").build()
        nm.createNotificationChannel(hanimeDownloadChannel)

        val appUpdateChannel = NotificationChannelCompat.Builder(
            UPDATE_NOTIFICATION_CHANNEL,
            NotificationManagerCompat.IMPORTANCE_HIGH
        ).setName("App Update").build()
        nm.createNotificationChannel(appUpdateChannel)
    }
    fun switchLauncher(alias: String) {
        val pm = packageManager

        val allAliases = listOf(
            "com.yenaly.han1meviewer.LauncherAliasDefault",
            "com.yenaly.han1meviewer.LauncherFakeCalc",
            "com.yenaly.han1meviewer.LauncherFakeCornhub",
            "com.yenaly.han1meviewer.LauncherFakeXxt"
        )

        allAliases.forEach { a ->
            val state = if (a == alias)
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED

            pm.setComponentEnabledSetting(
                ComponentName(this, a),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
