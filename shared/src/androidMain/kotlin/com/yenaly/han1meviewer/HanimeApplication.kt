package com.yenaly.han1meviewer

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Process
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
import com.yenaly.han1meviewer.logic.network.AndroidCloudflareSolver
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.logic.network.OkHttpNetworkConfig
import com.yenaly.han1meviewer.logic.network.plugin.CloudflareChallengeHandler
import com.yenaly.han1meviewer.mmkv.initializeMMKV
import com.yenaly.han1meviewer.mmkv.migrateSharedPreferencesToMMKV
import com.yenaly.han1meviewer.ui.activity.MainActivity
import com.yenaly.han1meviewer.ui.viewmodel.AppViewModel
import com.yenaly.han1meviewer.ui.viewmodel.VideoPlatformBridge
import com.yenaly.han1meviewer.util.AndroidAppContext
import com.yenaly.han1meviewer.util.AnimeShaders
import com.yenaly.han1meviewer.util.LanguageHelper
import com.yenaly.han1meviewer.util.ThemeUtils
import com.yenaly.han1meviewer.util.dpPx
import `is`.xyz.mpv.MPVLib
import okhttp3.Cache
import java.io.File
import java.net.ProxySelector

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/08 008 17:32
 */
class HanimeApplication : Application() {

    companion object {
        const val TAG = "HanimeApplication"

        private const val HTTP_CACHE_SIZE = 10L * 1024 * 1024
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

    private fun isMainProcess(): Boolean {
        val pid = Process.myPid()
        val am = getSystemService(android.app.ActivityManager::class.java)
        return am?.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName == packageName
    }

    override fun onCreate() {
        super.onCreate()
        AndroidAppContext.initialize(this)
        VideoPlatformBridge.loadCachedVideo = { videoCode ->
            HCacheManager.loadHanimeVideoInfo(this, videoCode)
        }
        VideoPlatformBridge.defaultPlayerHeightPx = { 250.dpPx }
        // MMKV 必须早于任何 Preferences 访问：Preferences 里的几个 StateFlow 一被碰到就会读盘。
        // 放在 isMainProcess() 判断之前，因为非主进程（下载 worker 等）同样会读 Preferences。
        initializeMMKV()
        if (!isMainProcess()) return
        // 迁移只在主进程做一次，读写旧 SharedPreferences 不适合多进程并发。
        migrateSharedPreferencesToMMKV(this)
        // 过 Cloudflare 盾要拉起 Activity 跑 WebView，只有 Android 有；
        // 检测和重发的逻辑在 commonMain 的 CloudflareChallenge 插件里。
        CloudflareChallengeHandler.solver = AndroidCloudflareSolver(this)
        // OkHttp 的磁盘缓存目录要 Context，androidJvmMain 拿不到，从这里注入。
        OkHttpNetworkConfig.cacheProvider = {
            Cache(directory = File(cacheDir, "http_cache"), maxSize = HTTP_CACHE_SIZE)
        }
        initCrashX()
        ThemeUtils.applyDarkModeFromPreferences(this)
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
