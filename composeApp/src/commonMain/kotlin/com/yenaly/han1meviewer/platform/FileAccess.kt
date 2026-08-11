package com.yenaly.han1meviewer.platform

/** Opaque platform file or document reference. It must not be interpreted as a filesystem path. */
data class PlatformFileRef(val value: String)

data class DownloadDirectorySelection(
    val reference: PlatformFileRef?,
    val confirmationText: String,
)

data class DownloadMigrationProgress(
    val migrated: Int,
    val total: Int,
)

/**
 * Boundary for the existing download-file operations.
 *
 * Android keeps ownership of SAF grants, DocumentFile and ContentResolver. Callers only retain
 * opaque references and stable download identifiers.
 */
interface FileAccess {
    /** Opens the platform directory picker and persists the selected download-directory grant. */
    suspend fun chooseDownloadDirectory(): PlatformActionResult<DownloadDirectorySelection>

    fun selectedDownloadDirectory(): PlatformActionResult<PlatformFileRef?>

    fun selectedDownloadDirectoryDisplayName(): PlatformActionResult<String?>

    fun privateDownloadDirectoryDisplayName(): PlatformActionResult<String>

    fun restoreDefaultDownloadDirectory(): PlatformActionResult<Unit>

    fun hasDownloadDirectoryAccess(): PlatformActionResult<Boolean>

    suspend fun scanAndImportDownloads(): PlatformActionResult<Unit>

    /** Starts the existing Android migration and forwards its existing progress callbacks. */
    fun migratePrivateDownloads(
        onProgress: (DownloadMigrationProgress) -> Unit,
    ): PlatformActionResult<Unit>

    fun deleteDownloadedVideoFolder(videoCode: String): PlatformActionResult<Unit>

    fun openDownloadedVideo(
        reference: PlatformFileRef,
        onFileNotFound: () -> Unit = {},
    ): PlatformActionResult<Unit>
}

expect fun fileAccess(): FileAccess

internal object UnsupportedFileAccess : FileAccess {
    override suspend fun chooseDownloadDirectory(): PlatformActionResult<DownloadDirectorySelection> =
        unavailable()

    override fun selectedDownloadDirectory(): PlatformActionResult<PlatformFileRef?> = unavailable()

    override fun selectedDownloadDirectoryDisplayName(): PlatformActionResult<String?> =
        unavailable()

    override fun privateDownloadDirectoryDisplayName(): PlatformActionResult<String> =
        unavailable()

    override fun restoreDefaultDownloadDirectory(): PlatformActionResult<Unit> = unavailable()

    override fun hasDownloadDirectoryAccess(): PlatformActionResult<Boolean> = unavailable()

    override suspend fun scanAndImportDownloads(): PlatformActionResult<Unit> = unavailable()

    override fun migratePrivateDownloads(
        onProgress: (DownloadMigrationProgress) -> Unit,
    ): PlatformActionResult<Unit> = unavailable()

    override fun deleteDownloadedVideoFolder(videoCode: String): PlatformActionResult<Unit> =
        unavailable()

    override fun openDownloadedVideo(
        reference: PlatformFileRef,
        onFileNotFound: () -> Unit,
    ): PlatformActionResult<Unit> = unavailable()

    private fun unavailable(): PlatformActionResult<Nothing> =
        PlatformActionResult.Unavailable(UnavailableReason.UnsupportedPlatform)
}
