package io.github.darriousliu.han1meviewer

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.developer.crashx.config.CrashConfig
import com.google.android.material.color.DynamicColors
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.google.firebase.database.database
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import io.github.darriousliu.han1meviewer.di.initKoin
import io.github.darriousliu.han1meviewer.logic.network.HProxySelector
import io.github.darriousliu.han1meviewer.mmkv.initializeMMKV
import io.github.darriousliu.han1meviewer.mmkv.migrateSharedPreferencesToMMKV
import io.github.darriousliu.han1meviewer.ui.activity.MainActivity
import io.github.darriousliu.han1meviewer.ui.viewmodel.AppViewModel
import io.github.darriousliu.han1meviewer.core.common.util.AndroidAppContext
import io.github.darriousliu.han1meviewer.core.common.util.isMainProcess
import io.github.darriousliu.han1meviewer.util.AnimeShaders
import io.github.darriousliu.han1meviewer.core.common.util.LanguageHelper
import io.github.darriousliu.han1meviewer.util.ThemeUtils
import io.github.darriousliu.han1meviewer.core.common.util.migrateAppLanguageToPlatformIfNeeded
import `is`.xyz.mpv.MPVLib
import org.koin.android.ext.koin.androidContext
import java.net.ProxySelector
import io.github.darriousliu.han1meviewer.core.common.BuildConfig
import io.github.darriousliu.han1meviewer.core.common.util.applicationContext
import io.github.darriousliu.han1meviewer.core.common.DOWNLOAD_NOTIFICATION_CHANNEL
import io.github.darriousliu.han1meviewer.core.common.UPDATE_NOTIFICATION_CHANNEL

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/08 008 17:32
 */
class HanimeApplication : Application() {

    companion object {
        const val TAG = "HanimeApplication"
    }

    private fun initCrashX() {
        CrashConfig.Builder.create()
            .backgroundMode(CrashConfig.BACKGROUND_MODE_SHOW_CUSTOM)
            .enabled(true)
            .includeDeviceInfo(true)
            .showErrorDetails(true)
            .showRestartButton(true)
            .showCloseButton(true)
            .showReportButton(true)
            .showCopyButtonInDetails(true)
            .logErrorOnRestart(true)
            .trackActivities(true)
            .minTimeBetweenCrashesMs(3000)
            .errorTitle(getString(R.string.crash_title))
            .errorDrawable(R.drawable.h_chan_cry)
            .errorMessage(getString(R.string.crash_message))
            .restartButtonText(getString(R.string.crash_restart))
            .closeButtonText(getString(R.string.crash_close))
            .detailsButtonText(getString(R.string.crash_details))
            .reportButtonText(getString(R.string.crash_report))
            .copyButtonText(getString(R.string.crash_copy))
            .restartActivity(MainActivity::class.java)
            .apply()
    }

    override fun onCreate() {
        super.onCreate()
        // 平台能力（视频缓存、公告、过盾、HTTP 磁盘缓存）都已改成 expect/actual，
        // 这里不再做任何 lambda 注入；actual 侧经 AndroidAppContext 拿 Context。
        AndroidAppContext.initialize(this)
        // MMKV 必须早于任何 Preferences 访问：Preferences 里的几个 StateFlow 一被碰到就会读盘。
        // 放在 isMainProcess 判断之前，因为非主进程（下载 worker 等）同样会读 Preferences。
        initializeMMKV()
        // 同样放在 isMainProcess 之前：worker 跑在别的进程里，也要能取到依赖。
        initKoin { androidContext(this@HanimeApplication) }
        if (!isMainProcess) return
        // 迁移只在主进程做一次，读写旧 SharedPreferences 不适合多进程并发。
        migrateSharedPreferencesToMMKV(this)
        initCrashX()
        ThemeUtils.applyDarkModeFromPreferences(this)
        migrateAppLanguageToPlatformIfNeeded(Preferences.appLanguage)
        if (Preferences.useDynamicColor){
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
        ProxySelector.setDefault(HProxySelector())
        HProxySelector.rebuildNetwork()
        initFirebase()
        initNotificationChannel()
        MPVLib.create(applicationContext)
        MPVLib.init()

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
        // 用于处理 Firebase Analytics 初始化
        Firebase.analytics.setAnalyticsCollectionEnabled(Preferences.isAnalyticsEnabled)
        // 用于处理 Firebase Crashlytics 初始化
        Firebase.crashlytics.apply {
            isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
            setCustomKeys {
                key(
                    FirebaseConstants.APP_LANGUAGE,
                    LanguageHelper.preferredLanguage.toLanguageTag()
                )
                key(
                    FirebaseConstants.VERSION_SOURCE,
                    BuildConfig.VERSION_SOURCE
                )
            }
        }
        // 用于处理 Firebase Remote Config 初始化
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3 * 60 * 60
                fetchTimeoutInSeconds = 10
            })
            setDefaultsAsync(FirebaseConstants.remoteConfigDefaults)
            fetchAndActivate().addOnCompleteListener {
                AppViewModel.getLatestVersion(delayMillis = 200)
            }
        }
        Firebase.database.setPersistenceEnabled(true)
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
            "io.github.darriousliu.han1meviewer.LauncherAliasDefault",
            "io.github.darriousliu.han1meviewer.LauncherFakeCalc",
            "io.github.darriousliu.han1meviewer.LauncherFakeCornhub",
            "io.github.darriousliu.han1meviewer.LauncherFakeXxt"
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
