package com.yenaly.han1meviewer.platform

import java.awt.Desktop
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.util.Locale
import kotlinx.coroutines.CancellationException

actual fun platformServices(): PlatformServices = JvmPlatformServices

private val JvmPlatformServices = PlatformServices(
    capabilities = PlatformCapabilities(
        supportsExternalNavigation = isBrowseSupported(),
        supportsClipboard = isClipboardSupported(),
        supportsTextSharing = false,
        supportsNotificationPermission = false,
        supportsAppInstallation = false,
        supportsCustomDownloadDirectory = false,
        supportsBackgroundDownloads = false,
        supportsHomeWidgetPinning = false,
        supportsPictureInPictureSettings = false,
    ),
    externalNavigator = JvmExternalNavigator,
    clipboard = JvmClipboardService,
    share = ShareService { _, _ -> unsupportedPlatform() },
    language = LanguageService {
        try {
            val locale = Locale.getDefault()
            PlatformLanguage(
                tag = locale.toLanguageTag().ifBlank { "und" },
                language = locale.language.ifBlank { "und" },
                country = locale.country,
            )
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

private object JvmExternalNavigator : ExternalNavigator {
    override fun open(uri: String): PlatformActionResult<Unit> {
        if (uri.isBlank()) return PlatformActionResult.Unavailable(UnavailableReason.InvalidInput)
        val target = try {
            URI(uri)
        } catch (_: Exception) {
            return PlatformActionResult.Unavailable(UnavailableReason.InvalidInput)
        }
        if (!isBrowseSupported()) return unsupportedPlatform()
        return jvmAction { Desktop.getDesktop().browse(target) }
    }
}

private object JvmClipboardService : ClipboardService {
    override fun readText(): PlatformActionResult<String?> {
        if (!isClipboardSupported()) return unsupportedPlatform()
        return jvmAction {
            Toolkit.getDefaultToolkit().systemClipboard.getContents(null)?.let { contents ->
                if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    contents.getTransferData(DataFlavor.stringFlavor)?.toString()
                } else {
                    null
                }
            }
        }
    }

    override fun writeText(text: String, label: String?): PlatformActionResult<Unit> {
        if (!isClipboardSupported()) return unsupportedPlatform()
        return jvmAction {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        }
    }
}

private fun isBrowseSupported(): Boolean = try {
    Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
} catch (_: Exception) {
    false
}

private fun isClipboardSupported(): Boolean = try {
    !GraphicsEnvironment.isHeadless() && Toolkit.getDefaultToolkit().systemClipboard != null
} catch (_: Exception) {
    false
}

private inline fun <T> jvmAction(action: () -> T): PlatformActionResult<T> = try {
    PlatformActionResult.Success(action())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    PlatformActionResult.Failure(error.message ?: error::class.simpleName.orEmpty(), error)
}

private fun unsupportedPlatform(): PlatformActionResult<Nothing> =
    PlatformActionResult.Unavailable(UnavailableReason.UnsupportedPlatform)
