package com.yenaly.han1meviewer.desktop.crash

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.IdentityHashMap

enum class DesktopCrashOrigin(val reportValue: String) {
    STARTUP("startup"),
    COMPOSE_WINDOW("compose-window"),
    ROOT_COROUTINE("root-coroutine"),
    UNCAUGHT_THREAD("uncaught-thread"),
    SECONDARY("secondary"),
}

data class DesktopCrashEnvironment(
    val applicationId: String?,
    val versionName: String?,
    val versionCode: String?,
    val commitSha: String?,
    val versionSource: String?,
    val debug: Boolean?,
    val osName: String?,
    val osVersion: String?,
    val osArchitecture: String?,
    val javaVersion: String?,
    val javaVendor: String?,
    val javaVmName: String?,
)

data class DesktopThrowableSnapshot(
    val type: String,
    val message: String?,
    val stackTrace: List<String>,
    val suppressed: List<DesktopThrowableSnapshot>,
    val cause: DesktopThrowableSnapshot?,
    val omittedStackFrames: Int,
    val omittedSuppressed: Int,
    val causeOmitted: Boolean,
    val note: String? = null,
)

data class DesktopCrashIncident(
    val timestamp: Instant,
    val origin: DesktopCrashOrigin,
    val threadName: String,
    val threadId: Long,
    val throwable: DesktopThrowableSnapshot,
)

@ConsistentCopyVisibility
data class DesktopCrashReport internal constructor(
    val incident: DesktopCrashIncident,
    val environment: DesktopCrashEnvironment,
    val rendered: String,
    val truncated: Boolean,
    internal val secondaryRendered: String,
    internal val secondaryTruncated: Boolean,
) {
    val timestamp: Instant get() = incident.timestamp
    val origin: DesktopCrashOrigin get() = incident.origin
    val threadName: String get() = incident.threadName
    val threadId: Long get() = incident.threadId

    companion object {
        const val MAX_RENDERED_UTF8_BYTES: Int = 256 * 1024
        internal const val MAX_SECONDARY_UTF8_BYTES: Int = 64 * 1024
    }
}

object DesktopCrashReportFactory {
    private const val REPORT_FORMAT_VERSION = 1
    private const val MAX_THROWABLE_DEPTH = 8
    private const val MAX_THROWABLE_NODES = 24
    private const val MAX_STACK_FRAMES = 64
    private const val MAX_SUPPRESSED_PER_THROWABLE = 4
    private const val MAX_SCALAR_INPUT_CHARS = 64 * 1024
    private const val MAX_SCALAR_OUTPUT_CHARS = 4 * 1024

    fun create(
        origin: DesktopCrashOrigin,
        throwable: Throwable,
        thread: Thread = Thread.currentThread(),
    ): DesktopCrashReport {
        val timestamp = runCatching { Instant.now() }.getOrDefault(Instant.EPOCH)
        val incident = DesktopCrashIncident(
            timestamp = timestamp,
            origin = origin,
            threadName = sanitizeScalar(runCatching { thread.name }.getOrDefault("unknown")),
            threadId = runCatching { thread.threadId() }.getOrDefault(-1L),
            throwable = captureThrowable(throwable),
        )
        val environment = captureEnvironment()
        val unbounded = renderReport(incident, environment)
        val rendered = fitUtf8(unbounded, DesktopCrashReport.MAX_RENDERED_UTF8_BYTES)
        val unboundedSecondary = renderSecondary(incident)
        val secondary = fitUtf8(
            unboundedSecondary,
            DesktopCrashReport.MAX_SECONDARY_UTF8_BYTES,
        )
        return DesktopCrashReport(
            incident = incident,
            environment = environment,
            rendered = rendered,
            truncated = rendered.toByteArray(StandardCharsets.UTF_8).size <
                unbounded.toByteArray(StandardCharsets.UTF_8).size,
            secondaryRendered = secondary,
            secondaryTruncated = secondary.toByteArray(StandardCharsets.UTF_8).size <
                unboundedSecondary.toByteArray(StandardCharsets.UTF_8).size,
        )
    }

    private fun captureEnvironment(): DesktopCrashEnvironment = DesktopCrashEnvironment(
        applicationId = safeProperty("hanime.build.applicationId"),
        versionName = safeProperty("hanime.build.versionName"),
        versionCode = safeProperty("hanime.build.versionCode"),
        commitSha = safeProperty("hanime.build.commitSha"),
        versionSource = safeProperty("hanime.build.versionSource"),
        debug = safeProperty("hanime.build.debug")?.toBooleanStrictOrNull(),
        osName = safeProperty("os.name"),
        osVersion = safeProperty("os.version"),
        osArchitecture = safeProperty("os.arch"),
        javaVersion = safeProperty("java.version"),
        javaVendor = safeProperty("java.vendor"),
        javaVmName = safeProperty("java.vm.name"),
    )

    private fun safeProperty(name: String): String? = runCatching {
        System.getProperty(name)
            ?.takeIf(String::isNotBlank)
            ?.let(::sanitizeScalar)
    }.getOrNull()

    private fun captureThrowable(root: Throwable): DesktopThrowableSnapshot {
        val seen = IdentityHashMap<Throwable, Unit>()
        val budget = ThrowableBudget(MAX_THROWABLE_NODES)
        return captureThrowable(root, depth = 0, seen = seen, budget = budget)
    }

    private fun captureThrowable(
        throwable: Throwable,
        depth: Int,
        seen: IdentityHashMap<Throwable, Unit>,
        budget: ThrowableBudget,
    ): DesktopThrowableSnapshot {
        val type = sanitizeScalar(
            runCatching { throwable.javaClass.name }.getOrDefault("java.lang.Throwable"),
        )
        if (seen.put(throwable, Unit) != null) {
            return placeholderThrowable(type, "cycle omitted")
        }
        if (!budget.takeNode()) {
            return placeholderThrowable(type, "throwable node limit reached")
        }

        val sourceStack = runCatching { throwable.stackTrace.toList() }.getOrDefault(emptyList())
        val stack = sourceStack
            .take(MAX_STACK_FRAMES)
            .map { frame -> sanitizeScalar(runCatching { frame.toString() }.getOrDefault("unknown")) }
        val sourceCause = runCatching { throwable.cause }.getOrNull()
        val cause = if (sourceCause != null && depth < MAX_THROWABLE_DEPTH && budget.hasNodes()) {
            captureThrowable(sourceCause, depth + 1, seen, budget)
        } else {
            null
        }
        val sourceSuppressed = runCatching { throwable.suppressed.toList() }.getOrDefault(emptyList())
        val suppressed = if (depth < MAX_THROWABLE_DEPTH) {
            sourceSuppressed
                .asSequence()
                .take(MAX_SUPPRESSED_PER_THROWABLE)
                .takeWhile { budget.hasNodes() }
                .map { captureThrowable(it, depth + 1, seen, budget) }
                .toList()
        } else {
            emptyList()
        }

        return DesktopThrowableSnapshot(
            type = type,
            message = runCatching { throwable.message }.getOrNull()?.let(::sanitizeScalar),
            stackTrace = stack,
            suppressed = suppressed,
            cause = cause,
            omittedStackFrames = (sourceStack.size - stack.size).coerceAtLeast(0),
            omittedSuppressed = (sourceSuppressed.size - suppressed.size).coerceAtLeast(0),
            causeOmitted = sourceCause != null && cause == null,
        )
    }

    private fun placeholderThrowable(type: String, note: String): DesktopThrowableSnapshot =
        DesktopThrowableSnapshot(
            type = type,
            message = null,
            stackTrace = emptyList(),
            suppressed = emptyList(),
            cause = null,
            omittedStackFrames = 0,
            omittedSuppressed = 0,
            causeOmitted = true,
            note = note,
        )

    private fun renderReport(
        incident: DesktopCrashIncident,
        environment: DesktopCrashEnvironment,
    ): String = buildString {
        appendLine("Han1meViewer desktop crash report")
        appendLine("report_format=$REPORT_FORMAT_VERSION")
        appendLine("timestamp=${incident.timestamp}")
        appendLine("origin=${incident.origin.reportValue}")
        appendLine("thread=${incident.threadName}")
        appendLine("thread_id=${incident.threadId}")
        appendLine()
        appendLine("[environment]")
        appendEnvironment("application_id", environment.applicationId)
        appendEnvironment("version_name", environment.versionName)
        appendEnvironment("version_code", environment.versionCode)
        appendEnvironment("commit_sha", environment.commitSha)
        appendEnvironment("version_source", environment.versionSource)
        appendEnvironment("debug", environment.debug?.toString())
        appendEnvironment("os_name", environment.osName)
        appendEnvironment("os_version", environment.osVersion)
        appendEnvironment("os_arch", environment.osArchitecture)
        appendEnvironment("java_version", environment.javaVersion)
        appendEnvironment("java_vendor", environment.javaVendor)
        appendEnvironment("java_vm", environment.javaVmName)
        appendLine()
        appendLine("[exception]")
        appendThrowable(incident.throwable, prefix = "")
    }

    private fun renderSecondary(incident: DesktopCrashIncident): String = buildString {
        appendLine()
        appendLine("===== secondary fatal incident =====")
        appendLine("timestamp=${incident.timestamp}")
        appendLine("origin=${incident.origin.reportValue}")
        appendLine("thread=${incident.threadName}")
        appendLine("thread_id=${incident.threadId}")
        appendThrowable(incident.throwable, prefix = "")
    }

    private fun StringBuilder.appendEnvironment(name: String, value: String?) {
        append(name)
        append('=')
        appendLine(value ?: "unknown")
    }

    private fun StringBuilder.appendThrowable(
        throwable: DesktopThrowableSnapshot,
        prefix: String,
        caption: String = "",
    ) {
        append(prefix)
        append(caption)
        append(throwable.type)
        throwable.message?.takeIf(String::isNotBlank)?.let { message ->
            append(": ")
            append(message)
        }
        throwable.note?.let { note ->
            append(" [")
            append(note)
            append(']')
        }
        appendLine()
        throwable.stackTrace.forEach { frame ->
            append(prefix)
            append("\tat ")
            appendLine(frame)
        }
        if (throwable.omittedStackFrames > 0) {
            append(prefix)
            append("\t... ")
            append(throwable.omittedStackFrames)
            appendLine(" stack frame(s) omitted")
        }
        throwable.suppressed.forEach { suppressed ->
            appendThrowable(suppressed, "$prefix\t", "Suppressed: ")
        }
        if (throwable.omittedSuppressed > 0) {
            append(prefix)
            append("\t... ")
            append(throwable.omittedSuppressed)
            appendLine(" suppressed exception(s) omitted")
        }
        throwable.cause?.let { cause ->
            appendThrowable(cause, prefix, "Caused by: ")
        }
        if (throwable.causeOmitted) {
            append(prefix)
            appendLine("Caused by: [omitted]")
        }
    }

    private fun sanitizeScalar(value: String): String {
        val boundedInput = value.take(MAX_SCALAR_INPUT_CHARS)
        return DesktopCrashRedactor.redact(boundedInput)
            .take(MAX_SCALAR_OUTPUT_CHARS)
            .replace('\u0000', '\uFFFD')
    }

    private class ThrowableBudget(private var remaining: Int) {
        fun hasNodes(): Boolean = remaining > 0

        fun takeNode(): Boolean {
            if (remaining <= 0) return false
            remaining -= 1
            return true
        }
    }
}

internal object DesktopCrashRedactor {
    private const val REDACTED = "<REDACTED>"
    private val unixUserPath = Regex("""(?i)(/(?:Users|home|var/home)/)[^/\s]+""")
    private val windowsUserPath = Regex("""(?i)([A-Z]:\\Users\\)[^\\\s]+""")
    private val urlUserInfo = Regex("""(?i)(://)[^/@\s:]+(?::[^/@\s]*)?@""")
    private val sensitiveUrlQuery = Regex(
        """(?i)([?&](?:access[_-]?token|refresh[_-]?token|token|api[_-]?key|apikey|secret|password|passwd|authorization|auth|session|cookie|code)=)[^&#\s]*""",
    )
    private val authorizationHeader = Regex("""(?im)(\bAuthorization\s*[:=]\s*)[^\r\n]+""")
    private val cookieHeader = Regex("""(?im)(\b(?:Set-Cookie|Cookie)\s*[:=]\s*)[^\r\n]+""")
    private val bearerToken = Regex("""(?i)\b(Bearer)\s+[A-Za-z0-9._~+/%=-]+""")
    private val sensitiveAssignment = Regex(
        """(?i)(["']?\b(?:authorization|cookie|token|secret|password|passwd|api[-_]?key|access[-_]?token|refresh[-_]?token|client[-_]?secret|session)\b["']?\s*[:=]\s*)(?:"[^"]*"|'[^']*'|[^\s,;&}]+)""",
    )
    private val githubToken = Regex("""\b(?:github_pat_[A-Za-z0-9_]{4,}|gh[pousr]_[A-Za-z0-9]{4,})\b""")
    private val jwt = Regex("""\beyJ[A-Za-z0-9_-]{4,}\.[A-Za-z0-9_-]{4,}\.[A-Za-z0-9_-]{4,}\b""")

    fun redact(source: String): String {
        var result = source
        val home = safeProperty("user.home")
        if (home != null && home.length > 3) {
            result = Regex(Regex.escape(home), RegexOption.IGNORE_CASE).replace(result, "<HOME>")
        }
        result = unixUserPath.replace(result) { match -> "${match.groupValues[1]}<USER>" }
        result = windowsUserPath.replace(result) { match -> "${match.groupValues[1]}<USER>" }

        val user = safeProperty("user.name")
        if (user != null && user.length >= 3) {
            result = Regex(Regex.escape(user), RegexOption.IGNORE_CASE).replace(result, "<USER>")
        }

        result = urlUserInfo.replace(result) { match -> "${match.groupValues[1]}$REDACTED@" }
        result = sensitiveUrlQuery.replace(result) { match -> "${match.groupValues[1]}$REDACTED" }
        result = authorizationHeader.replace(result) { match -> "${match.groupValues[1]}$REDACTED" }
        result = cookieHeader.replace(result) { match -> "${match.groupValues[1]}$REDACTED" }
        result = bearerToken.replace(result) { match -> "${match.groupValues[1]} $REDACTED" }
        result = sensitiveAssignment.replace(result) { match ->
            "${match.groupValues[1]}$REDACTED"
        }
        result = githubToken.replace(result, REDACTED)
        result = jwt.replace(result, REDACTED)
        return result
    }

    private fun safeProperty(name: String): String? = runCatching {
        System.getProperty(name)?.takeIf(String::isNotBlank)
    }.getOrNull()
}

internal fun fitUtf8(
    source: String,
    maximumBytes: Int,
    marker: String = "\n[truncated]\n",
): String {
    require(maximumBytes >= 0) { "maximumBytes must not be negative" }
    if (source.toByteArray(StandardCharsets.UTF_8).size <= maximumBytes) return source

    val markerBytes = marker.toByteArray(StandardCharsets.UTF_8)
    if (markerBytes.size >= maximumBytes) {
        return utf8Prefix(marker, maximumBytes)
    }
    val contentLimit = maximumBytes - markerBytes.size
    return utf8Prefix(source, contentLimit) + marker
}

private fun utf8Prefix(source: String, maximumBytes: Int): String {
    if (maximumBytes <= 0 || source.isEmpty()) return ""
    var low = 0
    var high = source.length
    while (low < high) {
        val middle = (low + high + 1) ushr 1
        val safeMiddle = if (
            middle < source.length &&
            middle > 0 &&
            source[middle - 1].isHighSurrogate() &&
            source[middle].isLowSurrogate()
        ) {
            middle - 1
        } else {
            middle
        }
        val byteCount = source.substring(0, safeMiddle).toByteArray(StandardCharsets.UTF_8).size
        if (byteCount <= maximumBytes) {
            low = middle
        } else {
            high = middle - 1
        }
    }
    var end = low.coerceAtMost(source.length)
    if (end < source.length && end > 0 && source[end - 1].isHighSurrogate() && source[end].isLowSurrogate()) {
        end -= 1
    }
    while (end > 0 && source.substring(0, end).toByteArray(StandardCharsets.UTF_8).size > maximumBytes) {
        end -= 1
    }
    return source.substring(0, end)
}
