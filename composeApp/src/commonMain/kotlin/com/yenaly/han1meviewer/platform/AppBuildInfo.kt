package com.yenaly.han1meviewer.platform

/**
 * Variant and packaging data owned by the platform entry point.
 *
 * The KMP module deliberately has no generated build constants. Every platform shell must install
 * this value before starting application services.
 */
data class AppBuildInfo(
    val debug: Boolean,
    val applicationId: String,
    val versionCode: Int,
    val versionName: String,
    val commitSha: String,
    val versionSource: String,
    val githubToken: String,
    val searchYearRangeEnd: Int,
)

object AppBuildInfoProvider {
    private var installed: AppBuildInfo? = null

    val current: AppBuildInfo
        get() = checkNotNull(installed) {
            "AppBuildInfo must be installed by the platform entry point before business startup"
        }

    fun install(buildInfo: AppBuildInfo) {
        installed = buildInfo
    }
}
