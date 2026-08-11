package com.yenaly.han1meviewer.desktop.crash

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.nio.file.Files
import java.nio.file.Path

/** Result returned by an action exposed by the desktop crash page. */
sealed interface ActionResult {
    data object Success : ActionResult

    data class Failure(
        val reason: FailureReason,
        /** Exception type only: exception messages can contain local paths or process data. */
        val causeType: String? = null,
    ) : ActionResult
}

enum class FailureReason {
    ClipboardUnavailable,
    LogDirectoryUnavailable,
    OpenLogDirectoryUnsupported,
    OpenLogDirectoryFailed,
    RestartUnavailable,
    RestartFailed,
}

/**
 * An immutable snapshot of the current process command captured during installation.
 *
 * Command arguments deliberately have no accessor and are never rendered by [toString].
 */
class RestartCommand private constructor(
    command: List<String>,
) {
    private val command: List<String> = command.toList()

    val argumentCount: Int
        get() = (command.size - 1).coerceAtLeast(0)

    internal fun start(): ActionResult = safely(
        failureReason = FailureReason.RestartFailed,
    ) {
        ProcessBuilder(command).inheritIO().start()
        ActionResult.Success
    }

    override fun toString(): String =
        "RestartCommand(command=<redacted>, argumentCount=$argumentCount)"

    companion object {
        /** Call from the fatal-controller installation path, before application startup. */
        fun captureAtInstall(): RestartCommand? = try {
            val processInfo = ProcessHandle.current().info()
            val executable = processInfo.command().orElse(null)
                ?.takeIf(String::isNotBlank)
                ?: return null
            val arguments = processInfo.arguments().orElse(emptyArray())
            RestartCommand(listOf(executable, *arguments))
        } catch (_: Throwable) {
            null
        }
    }
}

/**
 * Side effects used by the standalone desktop crash page.
 *
 * All public operations convert failures to [ActionResult]; none propagates an exception to UI.
 */
class DesktopCrashActions private constructor(
    private val restartCommand: RestartCommand?,
) {
    val canRestart: Boolean
        get() = restartCommand != null

    /** Safe metadata for a crash report; the executable and its arguments are never included. */
    val restartReportSummary: String
        get() = if (restartCommand == null) {
            "restartCommandAvailable=false"
        } else {
            "restartCommandAvailable=true; argumentCount=${restartCommand.argumentCount}"
        }

    fun copyReportToClipboard(report: String): ActionResult = safely(
        failureReason = FailureReason.ClipboardUnavailable,
    ) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(report), null)
        ActionResult.Success
    }

    fun openLogDirectory(logDirectory: Path): ActionResult {
        val directory = try {
            logDirectory.toAbsolutePath().normalize().also(Files::createDirectories)
        } catch (failure: Throwable) {
            return failure.asActionFailure(FailureReason.LogDirectoryUnavailable)
        }

        val desktopFailure = tryOpenWithDesktop(directory)
        if (desktopFailure == null) return ActionResult.Success

        val fallback = openDirectoryCommand(directory)
            ?: return ActionResult.Failure(FailureReason.OpenLogDirectoryUnsupported)

        return safely(FailureReason.OpenLogDirectoryFailed) {
            ProcessBuilder(fallback).start()
            ActionResult.Success
        }
    }

    /** Must only be invoked from the explicit restart button callback. */
    fun restartOnUserRequest(): ActionResult = restartCommand?.start()
        ?: ActionResult.Failure(FailureReason.RestartUnavailable)

    override fun toString(): String = "DesktopCrashActions(restartCommand=<redacted>)"

    companion object {
        /** Captures restart data now; construction itself is guaranteed not to throw. */
        fun install(): DesktopCrashActions = try {
            DesktopCrashActions(RestartCommand.captureAtInstall())
        } catch (_: Throwable) {
            DesktopCrashActions(restartCommand = null)
        }
    }
}

private fun tryOpenWithDesktop(directory: Path): Throwable? = try {
    if (!Desktop.isDesktopSupported()) {
        UnsupportedOperationException("Desktop API is unavailable")
    } else {
        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.OPEN)) {
            UnsupportedOperationException("Desktop OPEN action is unavailable")
        } else {
            desktop.open(directory.toFile())
            null
        }
    }
} catch (failure: Throwable) {
    failure
}

private fun openDirectoryCommand(directory: Path): List<String>? {
    val absolutePath = directory.toString()
    val osName = try {
        System.getProperty("os.name").orEmpty().lowercase()
    } catch (_: Throwable) {
        return null
    }

    return when {
        osName.contains("mac") || osName.contains("darwin") ->
            listOf("/usr/bin/open", absolutePath)

        osName.contains("win") ->
            listOf("explorer.exe", absolutePath)

        osName.contains("linux") || osName.contains("unix") ->
            listOf("xdg-open", absolutePath)

        else -> null
    }
}

private inline fun safely(
    failureReason: FailureReason,
    action: () -> ActionResult,
): ActionResult = try {
    action()
} catch (failure: Throwable) {
    failure.asActionFailure(failureReason)
}

private fun Throwable.asActionFailure(reason: FailureReason): ActionResult.Failure =
    ActionResult.Failure(
        reason = reason,
        causeType = this::class.qualifiedName ?: this::class.simpleName,
    )
