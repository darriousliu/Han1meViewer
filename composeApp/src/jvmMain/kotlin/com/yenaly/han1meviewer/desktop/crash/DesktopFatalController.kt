package com.yenaly.han1meviewer.desktop.crash

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import java.awt.EventQueue
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal data class DesktopFatalIncident(
    val report: DesktopCrashReport,
    val persistence: DesktopCrashStoreResult,
    val logDirectory: java.nio.file.Path,
    val logFile: java.nio.file.Path?,
)

private enum class DesktopFatalPhase {
    NORMAL,
    CRASH_PAGE,
    CLOSED,
}

/**
 * Owns the process-wide fatal error state for the Desktop entry point.
 *
 * Only the first failure becomes the primary report. A bounded number of later failures are
 * appended to the same log without replacing the root cause.
 */
internal class DesktopFatalController private constructor(
    private val previousUncaughtHandler: Thread.UncaughtExceptionHandler?,
    private val crashStore: DesktopCrashStore,
    val actions: DesktopCrashActions,
    private val dispatchToUi: (() -> Unit) -> Unit,
    private val terminateProcess: (Int) -> Unit,
) {
    private val stateLock = Any()
    private val primary = AtomicReference<DesktopFatalIncident?>()
    private val primaryThrowable = AtomicReference<Throwable?>()
    private val normalApplicationExit = AtomicReference<(() -> Unit)?>(null)
    private val normalExitScheduled = AtomicBoolean(false)
    private val emergencyExitStarted = AtomicBoolean(false)
    private val secondaryCount = AtomicInteger(0)

    @Volatile
    private var phase: DesktopFatalPhase = DesktopFatalPhase.NORMAL

    private lateinit var installedUncaughtHandler: Thread.UncaughtExceptionHandler

    val rootCoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        capture(DesktopCrashOrigin.ROOT_COROUTINE, throwable, Thread.currentThread())
    }

    fun createApplicationScope(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): CoroutineScope = CoroutineScope(
        SupervisorJob() + dispatcher + rootCoroutineExceptionHandler,
    )

    @OptIn(ExperimentalComposeUiApi::class)
    val normalWindowExceptionHandlerFactory = WindowExceptionHandlerFactory {
        WindowExceptionHandler { throwable ->
            capture(DesktopCrashOrigin.COMPOSE_WINDOW, throwable, Thread.currentThread())
            throw throwable
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    val crashWindowExceptionHandlerFactory = WindowExceptionHandlerFactory {
        WindowExceptionHandler { throwable ->
            handleCrashPageFailure()
            throw throwable
        }
    }

    /** Run the normal Compose application and, only after it has stopped, a fresh crash app. */
    fun runTwoPhase(
        normalApplication: () -> Unit,
        crashApplication: (DesktopFatalIncident) -> Unit,
    ): Int {
        try {
            normalApplication()
        } catch (failure: Throwable) {
            capture(DesktopCrashOrigin.STARTUP, failure, Thread.currentThread())
        }

        val incident = synchronized(stateLock) {
            primary.get()?.also { phase = DesktopFatalPhase.CRASH_PAGE }
                ?: run {
                    phase = DesktopFatalPhase.CLOSED
                    null
                }
        }

        if (incident == null) {
            restoreDefaultHandler()
            return NORMAL_EXIT_CODE
        }

        try {
            crashApplication(incident)
        } catch (_: Throwable) {
            handleCrashPageFailure()
        } finally {
            synchronized(stateLock) {
                phase = DesktopFatalPhase.CLOSED
            }
            restoreDefaultHandler()
        }
        return FATAL_EXIT_CODE
    }

    /** Bind Compose's exitApplication only while the normal application is alive. */
    fun bindNormalApplicationExit(exitApplication: () -> Unit): AutoCloseable {
        normalApplicationExit.set(exitApplication)
        if (primary.get() != null) requestNormalApplicationExit()
        return AutoCloseable {
            normalApplicationExit.compareAndSet(exitApplication, null)
        }
    }

    fun capture(
        origin: DesktopCrashOrigin,
        throwable: Throwable,
        thread: Thread = Thread.currentThread(),
    ): Boolean {
        if (throwable is CancellationException) return false

        val isPrimary = synchronized(stateLock) {
            if (phase == DesktopFatalPhase.CRASH_PAGE) {
                false
            } else if (phase == DesktopFatalPhase.CLOSED) {
                false
            } else if (primaryThrowable.get() === throwable) {
                false
            } else if (primary.get() == null) {
                val report = DesktopCrashReportFactory.create(origin, throwable, thread)
                val persistence = crashStore.persistPrimary(report)
                val incident = DesktopFatalIncident(
                    report = report,
                    persistence = persistence,
                    logDirectory = crashStore.logDirectory,
                    logFile = crashStore.logFile,
                )
                primaryThrowable.set(throwable)
                check(primary.compareAndSet(null, incident))
                true
            } else {
                appendSecondaryIfAllowed(origin, throwable, thread)
                false
            }
        }

        if (isPrimary) requestNormalApplicationExit()
        return isPrimary
    }

    internal fun primaryIncident(): DesktopFatalIncident? = primary.get()

    internal fun handleCrashPageFailure() {
        if (!emergencyExitStarted.compareAndSet(false, true)) {
            terminateProcess(FATAL_EXIT_CODE)
            return
        }
        terminateProcess(FATAL_EXIT_CODE)
    }

    private fun appendSecondaryIfAllowed(
        origin: DesktopCrashOrigin,
        throwable: Throwable,
        thread: Thread,
    ) {
        val index = secondaryCount.incrementAndGet()
        if (index <= MAX_SECONDARY_REPORTS) {
            crashStore.appendSecondary(origin, throwable, thread)
        }
    }

    private fun requestNormalApplicationExit() {
        val exitApplication = normalApplicationExit.get() ?: return
        if (!normalExitScheduled.compareAndSet(false, true)) return
        runCatching {
            dispatchToUi(exitApplication)
        }.onFailure {
            runCatching(exitApplication)
        }
    }

    private fun restoreDefaultHandler() {
        if (::installedUncaughtHandler.isInitialized &&
            Thread.getDefaultUncaughtExceptionHandler() === installedUncaughtHandler
        ) {
            Thread.setDefaultUncaughtExceptionHandler(previousUncaughtHandler)
        }
    }

    companion object {
        const val NORMAL_EXIT_CODE = 0
        const val FATAL_EXIT_CODE = 1
        private const val MAX_SECONDARY_REPORTS = 8

        /** Install before any build-info, Compose, DI, database, network, or WebView setup. */
        fun install(
            crashStore: DesktopCrashStore = DesktopCrashStore(),
            actions: DesktopCrashActions = DesktopCrashActions.install(),
            dispatchToUi: (() -> Unit) -> Unit = ::dispatchOnEventThread,
            terminateProcess: (Int) -> Unit = ::exitProcess,
        ): DesktopFatalController {
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            val controller = DesktopFatalController(
                previousUncaughtHandler = previousHandler,
                crashStore = crashStore,
                actions = actions,
                dispatchToUi = dispatchToUi,
                terminateProcess = terminateProcess,
            )
            controller.installedUncaughtHandler = Thread.UncaughtExceptionHandler { thread, failure ->
                if (controller.phase == DesktopFatalPhase.CRASH_PAGE) {
                    controller.handleCrashPageFailure()
                } else {
                    controller.capture(DesktopCrashOrigin.UNCAUGHT_THREAD, failure, thread)
                }
            }
            Thread.setDefaultUncaughtExceptionHandler(controller.installedUncaughtHandler)
            return controller
        }

        private fun dispatchOnEventThread(action: () -> Unit) {
            if (EventQueue.isDispatchThread()) {
                action()
            } else {
                EventQueue.invokeLater(action)
            }
        }
    }
}
