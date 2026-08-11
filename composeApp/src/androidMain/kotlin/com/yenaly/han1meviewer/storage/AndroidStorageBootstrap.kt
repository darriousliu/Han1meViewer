package com.yenaly.han1meviewer.storage

import android.content.Context
import android.util.Log
import com.ctrip.flight.mmkv.initialize as initializeMmkv

/** Initializes MMKV, migrates legacy preferences, then publishes the process-wide storage graph. */
object AndroidStorageBootstrap {
    private const val TAG = "AndroidStorageBootstrap"

    private val initializationLock = Any()

    @Volatile
    private var initialized = false

    val isInitialized: Boolean
        get() = initialized && AppStorage.isInstalled

    fun ensureInitialized(context: Context) {
        if (isInitialized) return

        synchronized(initializationLock) {
            if (isInitialized) return
            if (AppStorage.isInstalled) {
                initialized = true
                return
            }

            val applicationContext = context.applicationContext
            val rootDirectory = initializeMmkv(applicationContext)
            val issueReporter = StorageIssueReporter(::reportIssue)
            val preparedStorage = AppStorage.prepare()
            val migration = try {
                val result = LegacySharedPreferencesMigration.migrate(
                    context = applicationContext,
                    preparedStorage = preparedStorage,
                    issueReporter = issueReporter,
                )
                AppStorage.install(preparedStorage, issueReporter)
                result
            } catch (throwable: Throwable) {
                AppStorage.abortPreparation(preparedStorage)
                throw throwable
            }

            initialized = true

            Log.i(
                TAG,
                "MMKV initialized at $rootDirectory; " +
                    "migrationCompleted=${!migration.alreadyCompleted}, " +
                    "migrated=${migration.migratedKeys.size}, " +
                    "preserved=${migration.preservedTargetKeys.size}, " +
                    "invalid=${migration.ignoredInvalidKeys.size}",
            )
        }
    }

    private fun reportIssue(issue: StorageIssue) {
        val location = buildString {
            append(issue.owner.name)
            issue.keyName?.let { append(':').append(it) }
            append('/').append(issue.operation.name)
        }
        if (issue.cause == null) {
            Log.w(TAG, "$location: ${issue.message}")
        } else {
            Log.w(TAG, "$location: ${issue.message}", issue.cause)
        }
    }
}
