package com.yenaly.han1meviewer.platform

import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import com.yenaly.han1meviewer.ui.navigation.settings.isPipPermissionGranted
import com.yenaly.han1meviewer.ui.navigation.settings.openPipPermissionSettings
import com.yenaly.han1meviewer.ui.widget.CheckInWidgetProvider
import com.yenaly.han1meviewer.util.installApkPackage
import com.yenaly.han1meviewer.util.requestPostNotificationPermission
import com.yenaly.yenaly_libs.ActivityManager
import com.yenaly.yenaly_libs.utils.LanguageHelper
import com.yenaly.yenaly_libs.utils.applicationContext
import com.yenaly.yenaly_libs.utils.browse
import com.yenaly.yenaly_libs.utils.copyTextToClipboard
import com.yenaly.yenaly_libs.utils.shareText as shareTextWithChooser
import com.yenaly.yenaly_libs.utils.textFromClipboard
import java.io.File
import kotlinx.coroutines.CancellationException

actual fun platformServices(): PlatformServices = AndroidPlatformServices

private val AndroidPlatformServices = PlatformServices(
    capabilities = PlatformCapabilities(
        supportsExternalNavigation = true,
        supportsClipboard = true,
        supportsTextSharing = true,
        supportsNotificationPermission = true,
        supportsAppInstallation = true,
        supportsCustomDownloadDirectory = true,
        supportsBackgroundDownloads = true,
        supportsHomeWidgetPinning = isHomeWidgetPinningSupported(),
        supportsPictureInPictureSettings = true,
    ),
    externalNavigator = AndroidExternalNavigator,
    clipboard = AndroidClipboardService,
    share = AndroidShareService,
    language = AndroidLanguageService,
    notificationPublisher = AndroidNotificationPublisher,
    appInstaller = AndroidAppInstaller,
    homeWidget = AndroidHomeWidgetController,
    pictureInPictureSettings = AndroidPictureInPictureSettings,
)

private object AndroidExternalNavigator : ExternalNavigator {
    override fun open(uri: String): PlatformActionResult<Unit> {
        if (uri.isBlank()) return PlatformActionResult.Unavailable(UnavailableReason.InvalidInput)
        return androidAction {
            val activity = ActivityManager.currentActivity.get()
            if (activity != null) activity browse uri else applicationContext browse uri
        }
    }
}

private object AndroidClipboardService : ClipboardService {
    override fun readText(): PlatformActionResult<String?> = androidAction {
        textFromClipboard?.toString()
    }

    override fun writeText(text: String, label: String?): PlatformActionResult<Unit> =
        androidAction { copyTextToClipboard(text, label) }
}

private object AndroidShareService : ShareService {
    override fun shareText(content: String, title: String?): PlatformActionResult<Unit> =
        androidAction { shareTextWithChooser(content, title) }
}

private object AndroidLanguageService : LanguageService {
    override fun preferredLanguage(): PlatformLanguage = try {
        val locale = LanguageHelper.preferredLanguage
        PlatformLanguage(
            tag = locale.toLanguageTag().ifBlank { "und" },
            language = locale.language.ifBlank { "und" },
            country = locale.country,
        )
    } catch (_: Exception) {
        PlatformLanguage(tag = "und", language = "und", country = "")
    }
}

private object AndroidNotificationPublisher : NotificationPublisher {
    override suspend fun requestPermission(): PlatformActionResult<Boolean> =
        try {
            PlatformActionResult.Success(applicationContext.requestPostNotificationPermission())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            error.toPlatformResult()
        }
}

private object AndroidAppInstaller : AppInstaller {
    override suspend fun requestInstall(packageFile: PlatformFileRef): PlatformActionResult<Unit> {
        if (packageFile.value.isBlank()) {
            return PlatformActionResult.Unavailable(UnavailableReason.InvalidInput)
        }
        return try {
            applicationContext.installApkPackage(File(packageFile.value))
            PlatformActionResult.Success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            error.toPlatformResult()
        }
    }
}

private object AndroidHomeWidgetController : HomeWidgetController {
    override fun requestPin(): PlatformActionResult<Boolean> {
        val manager = try {
            AppWidgetManager.getInstance(applicationContext)
        } catch (_: Exception) {
            return PlatformActionResult.Unavailable(UnavailableReason.SystemServiceUnavailable)
        }
        if (!manager.isRequestPinAppWidgetSupported) {
            return PlatformActionResult.Unavailable(UnavailableReason.UnsupportedFeature)
        }
        return androidAction {
            manager.requestPinAppWidget(
                ComponentName(applicationContext, CheckInWidgetProvider::class.java),
                null,
                null,
            )
        }
    }
}

private object AndroidPictureInPictureSettings : PictureInPictureSettings {
    override fun isPermissionGranted(): PlatformActionResult<Boolean> =
        androidAction { isPipPermissionGranted(applicationContext) }

    override fun openPermissionSettings(): PlatformActionResult<Unit> =
        androidAction { openPipPermissionSettings(applicationContext) }
}

private fun isHomeWidgetPinningSupported(): Boolean = try {
    AppWidgetManager.getInstance(applicationContext).isRequestPinAppWidgetSupported
} catch (_: Exception) {
    false
}

private inline fun <T> androidAction(action: () -> T): PlatformActionResult<T> = try {
    PlatformActionResult.Success(action())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    error.toPlatformResult()
}

private fun Exception.toPlatformResult(): PlatformActionResult<Nothing> = when (this) {
    is ActivityNotFoundException ->
        PlatformActionResult.Unavailable(UnavailableReason.NoCompatibleApplication)

    else -> PlatformActionResult.Failure(
        message ?: this::class.simpleName.orEmpty(),
        this,
    )
}
