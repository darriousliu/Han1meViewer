package com.yenaly.han1meviewer

import com.yenaly.han1meviewer.androidapp.BuildConfig
import com.yenaly.han1meviewer.platform.AppBuildInfo
import com.yenaly.han1meviewer.platform.AppBuildInfoProvider

/** Android packaging entry point. Application behavior lives in composeApp/androidMain. */
class HanimeApplication : AndroidHanimeApplication() {
    override fun onCreate() {
        AppBuildInfoProvider.install(
            AppBuildInfo(
                debug = BuildConfig.DEBUG,
                applicationId = BuildConfig.APPLICATION_ID,
                versionCode = BuildConfig.VERSION_CODE,
                versionName = BuildConfig.VERSION_NAME,
                commitSha = BuildConfig.COMMIT_SHA,
                versionSource = BuildConfig.VERSION_SOURCE,
                githubToken = BuildConfig.HA_GITHUB_TOKEN,
                searchYearRangeEnd = BuildConfig.SEARCH_YEAR_RANGE_END,
            ),
        )
        super.onCreate()
    }
}
