package com.yenaly.han1meviewer.platform

sealed interface PlatformActionResult<out T> {
    data class Success<T>(val value: T) : PlatformActionResult<T>

    data class Unavailable(val reason: UnavailableReason) : PlatformActionResult<Nothing>

    data object Cancelled : PlatformActionResult<Nothing>

    data class Failure(
        val message: String,
        val cause: Throwable? = null,
    ) : PlatformActionResult<Nothing>
}

enum class UnavailableReason {
    UnsupportedPlatform,
    UnsupportedFeature,
    ActivityUnavailable,
    SystemServiceUnavailable,
    NoCompatibleApplication,
    PermissionDenied,
    InvalidInput,
}

data class PlatformCapabilities(
    val supportsExternalNavigation: Boolean,
    val supportsClipboard: Boolean,
    val supportsTextSharing: Boolean,
    val supportsNotificationPermission: Boolean,
    val supportsAppInstallation: Boolean,
    val supportsCustomDownloadDirectory: Boolean,
    val supportsBackgroundDownloads: Boolean,
    val supportsHomeWidgetPinning: Boolean,
    val supportsPictureInPictureSettings: Boolean,
)

fun interface ExternalNavigator {
    fun open(uri: String): PlatformActionResult<Unit>
}

interface ClipboardService {
    fun readText(): PlatformActionResult<String?>

    fun writeText(text: String, label: String? = null): PlatformActionResult<Unit>
}

fun interface ShareService {
    fun shareText(content: String, title: String?): PlatformActionResult<Unit>
}

data class PlatformLanguage(
    val tag: String,
    val language: String,
    val country: String,
)

fun interface LanguageService {
    fun preferredLanguage(): PlatformLanguage
}

fun interface NotificationPublisher {
    suspend fun requestPermission(): PlatformActionResult<Boolean>
}

fun interface AppInstaller {
    suspend fun requestInstall(packageFile: PlatformFileRef): PlatformActionResult<Unit>
}

fun interface HomeWidgetController {
    fun requestPin(): PlatformActionResult<Boolean>
}

interface PictureInPictureSettings {
    fun isPermissionGranted(): PlatformActionResult<Boolean>

    fun openPermissionSettings(): PlatformActionResult<Unit>
}

data class PlatformServices(
    val capabilities: PlatformCapabilities,
    val externalNavigator: ExternalNavigator,
    val clipboard: ClipboardService,
    val share: ShareService,
    val language: LanguageService,
    val notificationPublisher: NotificationPublisher,
    val appInstaller: AppInstaller,
    val homeWidget: HomeWidgetController,
    val pictureInPictureSettings: PictureInPictureSettings,
)

expect fun platformServices(): PlatformServices
