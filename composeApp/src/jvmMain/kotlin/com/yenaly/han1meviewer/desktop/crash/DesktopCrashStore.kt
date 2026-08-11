package com.yenaly.han1meviewer.desktop.crash

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.StandardOpenOption.WRITE
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
import java.nio.file.attribute.PosixFilePermissions
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

enum class DesktopCrashStoreOperation {
    CREATE_DIRECTORY,
    WRITE_PRIMARY,
    APPEND_SECONDARY,
    NO_PRIMARY_LOG,
}

sealed interface DesktopCrashStoreResult {
    val path: Path?

    data class Written(
        override val path: Path,
        val bytesWritten: Int,
        val truncated: Boolean,
    ) : DesktopCrashStoreResult

    data class Failed(
        val operation: DesktopCrashStoreOperation,
        override val path: Path?,
        val message: String,
    ) : DesktopCrashStoreResult
}

class DesktopCrashStore(
    val logDirectory: Path = defaultLogDirectory(),
) {
    @Volatile
    var logFile: Path? = null
        private set

    private val writeLock = ReentrantLock()

    fun persistPrimary(report: DesktopCrashReport): DesktopCrashStoreResult = writeLock.withLock {
        val directoryFailure = createLogDirectory()
        if (directoryFailure != null) return@withLock directoryFailure

        val bytes = report.rendered.toByteArray(StandardCharsets.UTF_8)
        repeat(MAX_FILENAME_ATTEMPTS) { attempt ->
            val candidate = logDirectory.resolve(fileName(report, attempt))
            try {
                writeNewFile(candidate, bytes)
                applyOwnerOnlyPermissions(candidate)
                logFile = candidate
                pruneOldCrashLogs(candidate)
                return@withLock DesktopCrashStoreResult.Written(
                    path = candidate,
                    bytesWritten = bytes.size,
                    truncated = report.truncated,
                )
            } catch (_: FileAlreadyExistsException) {
                // Millisecond timestamps can collide when multiple fatal paths race.
            } catch (failure: Throwable) {
                return@withLock failureResult(
                    operation = DesktopCrashStoreOperation.WRITE_PRIMARY,
                    path = candidate,
                    failure = failure,
                )
            }
        }
        DesktopCrashStoreResult.Failed(
            operation = DesktopCrashStoreOperation.WRITE_PRIMARY,
            path = logDirectory,
            message = "Unable to allocate a unique crash log name",
        )
    }

    fun appendSecondary(report: DesktopCrashReport): DesktopCrashStoreResult = writeLock.withLock {
        val target = logFile ?: return@withLock DesktopCrashStoreResult.Failed(
            operation = DesktopCrashStoreOperation.NO_PRIMARY_LOG,
            path = null,
            message = "Primary crash log has not been persisted",
        )
        try {
            appendBounded(target, report.secondaryRendered, report.secondaryTruncated)
        } catch (failure: Throwable) {
            failureResult(
                operation = DesktopCrashStoreOperation.APPEND_SECONDARY,
                path = target,
                failure = failure,
            )
        }
    }

    fun appendSecondary(
        origin: DesktopCrashOrigin,
        throwable: Throwable,
        thread: Thread = Thread.currentThread(),
    ): DesktopCrashStoreResult = runCatching {
        DesktopCrashReportFactory.create(origin, throwable, thread)
    }.fold(
        onSuccess = ::appendSecondary,
        onFailure = { failure ->
            failureResult(
                operation = DesktopCrashStoreOperation.APPEND_SECONDARY,
                path = logFile,
                failure = failure,
            )
        },
    )

    private fun createLogDirectory(): DesktopCrashStoreResult.Failed? = try {
        Files.createDirectories(logDirectory)
        null
    } catch (failure: Throwable) {
        failureResult(
            operation = DesktopCrashStoreOperation.CREATE_DIRECTORY,
            path = logDirectory,
            failure = failure,
        )
    }

    private fun writeNewFile(path: Path, bytes: ByteArray) {
        val ownerOnly = PosixFilePermissions.asFileAttribute(OWNER_ONLY_PERMISSIONS)
        try {
            FileChannel.open(path, setOf(CREATE_NEW, WRITE), ownerOnly).use { channel ->
                val buffer = ByteBuffer.wrap(bytes)
                while (buffer.hasRemaining()) channel.write(buffer)
                channel.force(true)
            }
        } catch (_: UnsupportedOperationException) {
            Files.write(path, bytes, CREATE_NEW, WRITE)
        }
    }

    private fun appendBounded(
        path: Path,
        content: String,
        reportWasTruncated: Boolean,
    ): DesktopCrashStoreResult {
        FileChannel.open(path, WRITE).use { channel ->
            val fileLock = channel.tryLock() ?: return DesktopCrashStoreResult.Failed(
                operation = DesktopCrashStoreOperation.APPEND_SECONDARY,
                path = path,
                message = "Crash log is busy",
            )
            fileLock.use {
                val currentSize = channel.size()
                val remaining = (DesktopCrashReport.MAX_RENDERED_UTF8_BYTES.toLong() - currentSize)
                    .coerceAtLeast(0L)
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()
                if (remaining == 0) {
                    return DesktopCrashStoreResult.Written(
                        path = path,
                        bytesWritten = 0,
                        truncated = true,
                    )
                }

                val boundedContent = fitUtf8(content, remaining)
                val bytes = boundedContent.toByteArray(StandardCharsets.UTF_8)
                channel.position(currentSize)
                val buffer = ByteBuffer.wrap(bytes)
                while (buffer.hasRemaining()) channel.write(buffer)
                applyOwnerOnlyPermissions(path)
                return DesktopCrashStoreResult.Written(
                    path = path,
                    bytesWritten = bytes.size,
                    truncated = reportWasTruncated || boundedContent != content,
                )
            }
        }
    }

    private fun pruneOldCrashLogs(current: Path) {
        runCatching {
            Files.newDirectoryStream(logDirectory, "crash-*.log").use { entries ->
                entries
                    .asSequence()
                    .filter { path -> path != current && Files.isRegularFile(path, NOFOLLOW_LINKS) }
                    .sortedByDescending { path -> path.fileName.toString() }
                    .drop(MAX_RETAINED_LOGS - 1)
                    .forEach(Files::deleteIfExists)
            }
        }
    }

    private fun applyOwnerOnlyPermissions(path: Path) {
        runCatching { Files.setPosixFilePermissions(path, OWNER_ONLY_PERMISSIONS) }
    }

    private fun fileName(report: DesktopCrashReport, attempt: Int): String {
        val timestamp = FILE_TIMESTAMP_FORMAT.format(report.timestamp)
        val suffix = if (attempt == 0) "" else "-$attempt"
        return "crash-$timestamp$suffix.log"
    }

    private fun failureResult(
        operation: DesktopCrashStoreOperation,
        path: Path?,
        failure: Throwable,
    ): DesktopCrashStoreResult.Failed = DesktopCrashStoreResult.Failed(
        operation = operation,
        path = path,
        message = DesktopCrashRedactor.redact(
            failure.message?.take(FAILURE_MESSAGE_LIMIT) ?: failure.javaClass.simpleName,
        ),
    )

    companion object {
        private const val MAX_FILENAME_ATTEMPTS = 100
        private const val MAX_RETAINED_LOGS = 20
        private const val FAILURE_MESSAGE_LIMIT = 4 * 1024
        private val OWNER_ONLY_PERMISSIONS = setOf(OWNER_READ, OWNER_WRITE)
        private val FILE_TIMESTAMP_FORMAT = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(ZoneOffset.UTC)

        fun defaultLogDirectory(): Path = runCatching {
            val osName = safeProperty("os.name").orEmpty().lowercase()
            val home = safeProperty("user.home")?.let(::safePath)
            when {
                "mac" in osName || "darwin" in osName ->
                    (home ?: fallbackDirectory()).resolve("Library/Logs/Han1meViewer")

                "win" in osName -> {
                    val localAppData = safeEnvironment("LOCALAPPDATA")
                        ?.let(::safePath)
                        ?: home?.resolve("AppData/Local")
                        ?: fallbackDirectory()
                    localAppData.resolve("Han1meViewer/logs")
                }

                else -> {
                    val stateHome = safeEnvironment("XDG_STATE_HOME")
                        ?.let(::safePath)
                        ?.takeIf { path -> path.isAbsolute }
                        ?: home?.resolve(".local/state")
                        ?: fallbackDirectory()
                    stateHome.resolve("han1meviewer/logs")
                }
            }
        }.getOrElse { fallbackDirectory() }

        private fun fallbackDirectory(): Path =
            safeProperty("java.io.tmpdir")
                ?.let(::safePath)
                ?.resolve("Han1meViewer/logs")
                ?: Paths.get(".", "Han1meViewer", "logs")

        private fun safePath(value: String): Path? = runCatching { Paths.get(value) }.getOrNull()

        private fun safeProperty(name: String): String? = runCatching {
            System.getProperty(name)?.takeIf(String::isNotBlank)
        }.getOrNull()

        private fun safeEnvironment(name: String): String? = runCatching {
            System.getenv(name)?.takeIf(String::isNotBlank)
        }.getOrNull()
    }
}
