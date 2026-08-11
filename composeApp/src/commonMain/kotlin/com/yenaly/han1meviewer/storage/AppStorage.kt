package com.yenaly.han1meviewer.storage

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock

/** Process-wide storage graph. Platform entry points initialize MMKV before preparing this object. */
object AppStorage {
    private val lifecycleLock = reentrantLock()
    private var pending: PreparedAppStorage? = null
    private var installed: InstalledStores? = null
    private var installedIssueReporter: StorageIssueReporter = StorageIssueReporter.None

    val isInstalled: Boolean
        get() = lifecycleLock.withLock { installed != null }

    val auth: AuthStore
        get() = requireInstalled().auth

    val settings: SettingsStore
        get() = requireInstalled().settings

    val uiState: UiStateStore
        get() = requireInstalled().uiState

    val migrationMeta: MigrationMetaStore
        get() = requireInstalled().migrationMeta

    /** Called after the platform-specific MMKV initialize function and before legacy migration. */
    internal fun prepare(): PreparedAppStorage = lifecycleLock.withLock {
        check(installed == null) { "AppStorage is already installed" }
        check(pending == null) { "AppStorage has already prepared its MMKV handles" }
        PreparedAppStorage.open().also { pending = it }
    }

    /** Publishes owner stores only after platform migration and verification have completed. */
    internal fun install(
        prepared: PreparedAppStorage,
        issueReporter: StorageIssueReporter = StorageIssueReporter.None,
    ) = lifecycleLock.withLock {
        check(installed == null) { "AppStorage is already installed" }
        check(pending === prepared) { "Prepared storage does not belong to this AppStorage lifecycle" }
        installedIssueReporter = issueReporter
        installed = InstalledStores(
            auth = AuthStore(prepared.backend(StorageOwnerId.Auth), issueReporter),
            settings = SettingsStore(prepared.backend(StorageOwnerId.Settings), issueReporter),
            uiState = UiStateStore(prepared.backend(StorageOwnerId.UiState), issueReporter),
            migrationMeta = MigrationMetaStore(
                prepared.backend(StorageOwnerId.MigrationMeta),
                issueReporter,
            ),
        )
        pending = null
    }

    internal fun prepareAndInstall(
        issueReporter: StorageIssueReporter = StorageIssueReporter.None,
    ) {
        val prepared = prepare()
        try {
            install(prepared, issueReporter)
        } catch (throwable: Throwable) {
            abortPreparation(prepared)
            throw throwable
        }
    }

    /** Clears a failed preparation and closes its handles so initialization can be retried. */
    internal fun abortPreparation(prepared: PreparedAppStorage) = lifecycleLock.withLock {
        if (pending === prepared) {
            pending = null
            runCatching(prepared::close)
        }
    }

    fun refreshAll(): Map<StorageOwnerId, StorageRefreshResult> = mapOf(
        StorageOwnerId.Auth to auth.refreshAll(),
        StorageOwnerId.Settings to settings.refreshAll(),
        StorageOwnerId.UiState to uiState.refreshAll(),
        StorageOwnerId.MigrationMeta to migrationMeta.refreshAll(),
    )

    /** Snapshot compatible with BackupManager version 1, restricted to explicit allowlisted keys. */
    fun snapshotLegacyV1(): StorageSnapshotResult {
        val values = linkedMapOf<String, StoredValue>()
        val issues = mutableListOf<StorageIssue>()
        storesByOwner().values.forEach { store ->
            val snapshot = store.snapshot(StorageBackupPolicy.LegacyV1)
            snapshot.values.forEach { (name, value) ->
                check(values.put(name, value) == null) { "Duplicate legacy-v1 backup key $name" }
            }
            issues += snapshot.issues
        }
        return StorageSnapshotResult(values, issues)
    }

    /**
     * Restores only registered legacy-v1 keys. A backed-up enabled app lock is persisted as disabled
     * and returned as pending so UI authentication must explicitly enable it again.
     */
    fun restoreLegacyV1(values: Map<String, StoredValue>): StorageRestoreResult {
        val mutations = linkedMapOf<StorageOwnerId, MutableList<StorageMutation>>()
        val skipped = mutableListOf<StorageRestoreSkip>()
        var pendingAppLockRestore = false

        values.forEach { (name, storedValue) ->
            val key = StorageSchema.findLegacyV1Key(name)
            if (key == null) {
                val reason = if (StorageSchema.findKeys(name).isEmpty()) {
                    StorageRestoreSkipReason.UnknownKey
                } else {
                    StorageRestoreSkipReason.NotBackupEligible
                }
                skipped += StorageRestoreSkip(name, reason, "Key is not in the legacy-v1 allowlist")
                return@forEach
            }
            if (storedValue.kind != key.codec.storageKind) {
                skipped += StorageRestoreSkip(
                    keyName = name,
                    reason = StorageRestoreSkipReason.TypeMismatch,
                    message = "Expected ${key.codec.storageKind} but received ${storedValue.kind}",
                )
                reportRestoreIssue(key, "Backup value has the wrong type")
                return@forEach
            }

            val mutation = try {
                if (key === StorageSchema.Settings.useLockScreen) {
                    val requested = (storedValue as StoredValue.BooleanValue).value
                    pendingAppLockRestore = pendingAppLockRestore || requested
                    StorageMutation.Set(StorageSchema.Settings.useLockScreen, false)
                } else {
                    mutationFromStoredValue(key, storedValue)
                }
            } catch (exception: Exception) {
                skipped += StorageRestoreSkip(
                    keyName = name,
                    reason = StorageRestoreSkipReason.DecodeFailed,
                    message = exception.message ?: "Unable to decode backup value",
                )
                reportRestoreIssue(key, "Unable to decode backup value", exception)
                return@forEach
            }
            mutations.getOrPut(key.owner, ::mutableListOf) += mutation
        }

        val stores = storesByOwner()
        val batches = mutations.mapValues { (owner, ownerMutations) ->
            stores.getValue(owner).writeBatch(ownerMutations)
        }
        return StorageRestoreResult(
            batches = batches,
            skipped = skipped,
            pendingAppLockRestore = pendingAppLockRestore,
        )
    }

    private fun storesByOwner(): Map<StorageOwnerId, StorageOwner> = mapOf(
        StorageOwnerId.Auth to auth,
        StorageOwnerId.Settings to settings,
        StorageOwnerId.UiState to uiState,
        StorageOwnerId.MigrationMeta to migrationMeta,
    )

    private fun mutationFromStoredValue(
        key: StorageKey<*>,
        storedValue: StoredValue,
    ): StorageMutation = mutationFromStoredValueTyped(key, storedValue)

    private fun <T> mutationFromStoredValueTyped(
        key: StorageKey<T>,
        storedValue: StoredValue,
    ): StorageMutation = StorageMutation.Set(key, key.codec.decode(storedValue))

    private fun reportRestoreIssue(
        key: StorageKey<*>,
        message: String,
        cause: Throwable? = null,
    ) {
        runCatching {
            installedIssueReporter.report(
                StorageIssue(
                    owner = key.owner,
                    keyName = key.name,
                    operation = StorageOperation.Restore,
                    message = message,
                    cause = cause,
                ),
            )
        }
    }

    private fun requireInstalled(): InstalledStores = lifecycleLock.withLock {
        checkNotNull(installed) { "AppStorage has not been installed" }
    }

    private data class InstalledStores(
        val auth: AuthStore,
        val settings: SettingsStore,
        val uiState: UiStateStore,
        val migrationMeta: MigrationMetaStore,
    )
}
