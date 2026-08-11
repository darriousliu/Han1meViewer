package com.yenaly.han1meviewer.desktop

import com.yenaly.han1meviewer.desktop.crash.DesktopFatalController
import com.yenaly.han1meviewer.desktop.crash.runDesktopCrashApplication
import com.yenaly.han1meviewer.desktop.crash.runNormalDesktopApplication
import com.yenaly.han1meviewer.platform.AppBuildInfo
import com.yenaly.han1meviewer.platform.AppBuildInfoProvider
import com.yenaly.han1meviewer.storage.initializeDesktopStorage
import kotlin.system.exitProcess

fun main() {
    val fatalController = DesktopFatalController.install()
    val exitCode = fatalController.runTwoPhase(
        normalApplication = {
            AppBuildInfoProvider.install(readDesktopBuildInfo())
            initializeDesktopStorage()
            runNormalDesktopApplication(fatalController)
        },
        crashApplication = { incident ->
            runDesktopCrashApplication(fatalController, incident)
        },
    )
    exitProcess(exitCode)
}

private fun readDesktopBuildInfo(): AppBuildInfo = AppBuildInfo(
    debug = buildProperty("debug")?.toBooleanStrictOrNull() ?: true,
    applicationId = buildProperty("applicationId") ?: "com.yenaly.han1meviewer.desktop",
    versionCode = buildProperty("versionCode")?.toIntOrNull() ?: 0,
    versionName = buildProperty("versionName") ?: "0.0.0-dev",
    commitSha = buildProperty("commitSha") ?: "unknown",
    versionSource = buildProperty("versionSource") ?: "desktop",
    githubToken = runCatching { System.getenv("HA_GITHUB_TOKEN") }.getOrNull().orEmpty(),
    searchYearRangeEnd = buildProperty("searchYearRangeEnd")?.toIntOrNull()
        ?: runCatching { java.time.Year.now().value }.getOrDefault(2100),
)

private fun buildProperty(name: String): String? = runCatching {
    System.getProperty("hanime.build.$name")?.takeIf(String::isNotBlank)
}.getOrNull()
