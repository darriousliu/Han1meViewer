@file:Suppress("DEPRECATION")

package com.yenaly.han1meviewer.platform

import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import kotlinx.coroutines.CancellationException

actual fun platformServices(): PlatformServices = IosPlatformServices

private val IosPlatformServices = PlatformServices(
    capabilities = PlatformCapabilities(
        supportsExternalNavigation = true,
        supportsClipboard = true,
        supportsTextSharing = false,
        supportsNotificationPermission = false,
        supportsAppInstallation = false,
        supportsCustomDownloadDirectory = false,
        supportsBackgroundDownloads = false,
        supportsHomeWidgetPinning = false,
        supportsPictureInPictureSettings = false,
    ),
    externalNavigator = IosExternalNavigator,
    clipboard = object : ClipboardService {
        override fun readText(): PlatformActionResult<String?> = iosAction {
            UIPasteboard.generalPasteboard.string
        }

        override fun writeText(text: String, label: String?): PlatformActionResult<Unit> =
            iosAction { UIPasteboard.generalPasteboard.string = text }
    },
    share = ShareService { _, _ -> unsupportedPlatform() },
    language = LanguageService {
        try {
            NSUserDefaults.standardUserDefaults
                .stringArrayForKey("AppleLanguages")
                ?.firstOrNull()
                ?.toString()
                .orEmpty()
                .ifBlank { "und" }
                .toPlatformLanguage()
        } catch (_: Exception) {
            PlatformLanguage(tag = "und", language = "und", country = "")
        }
    },
    notificationPublisher = NotificationPublisher { unsupportedPlatform() },
    appInstaller = AppInstaller { unsupportedPlatform() },
    homeWidget = HomeWidgetController { unsupportedPlatform() },
    pictureInPictureSettings = object : PictureInPictureSettings {
        override fun isPermissionGranted(): PlatformActionResult<Boolean> = unsupportedPlatform()

        override fun openPermissionSettings(): PlatformActionResult<Unit> = unsupportedPlatform()
    },
)

private fun String.toPlatformLanguage(): PlatformLanguage {
    val subtags = replace('_', '-').split('-').filter { it.isNotBlank() }
    val language = subtags.firstOrNull()?.lowercase().orEmpty().ifBlank { "und" }
    val country = subtags.drop(1).firstOrNull { subtag ->
        (subtag.length == 2 && subtag.all { it.isLetter() }) ||
            (subtag.length == 3 && subtag.all { it.isDigit() })
    }?.uppercase().orEmpty()
    return PlatformLanguage(
        tag = ifBlank { "und" },
        language = language,
        country = country,
    )
}

private object IosExternalNavigator : ExternalNavigator {
    override fun open(uri: String): PlatformActionResult<Unit> {
        if (uri.isBlank()) return PlatformActionResult.Unavailable(UnavailableReason.InvalidInput)
        val target = NSURL.URLWithString(uri)
            ?: return PlatformActionResult.Unavailable(
                UnavailableReason.InvalidInput,
            )
        return try {
            if (UIApplication.sharedApplication.openURL(target)) {
                PlatformActionResult.Success(Unit)
            } else {
                PlatformActionResult.Unavailable(
                    UnavailableReason.NoCompatibleApplication,
                )
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            PlatformActionResult.Failure(
                error.message ?: error::class.simpleName.orEmpty(),
                error,
            )
        }
    }
}

private inline fun <T> iosAction(action: () -> T): PlatformActionResult<T> = try {
    PlatformActionResult.Success(action())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    PlatformActionResult.Failure(error.message ?: error::class.simpleName.orEmpty(), error)
}

private fun unsupportedPlatform(): PlatformActionResult<Nothing> =
    PlatformActionResult.Unavailable(UnavailableReason.UnsupportedPlatform)
