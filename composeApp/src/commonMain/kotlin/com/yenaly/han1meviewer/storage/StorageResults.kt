package com.yenaly.han1meviewer.storage

enum class StorageOperation {
    Read,
    Encode,
    Write,
    Sync,
    Refresh,
    Snapshot,
    Restore,
}

data class StorageIssue(
    val owner: StorageOwnerId,
    val keyName: String?,
    val operation: StorageOperation,
    val message: String,
    val cause: Throwable? = null,
)

fun interface StorageIssueReporter {
    fun report(issue: StorageIssue)

    companion object {
        val None = StorageIssueReporter { }
    }
}

enum class StorageWriteFailureStage {
    Validation,
    Encode,
    Write,
    Sync,
}

sealed interface StorageWriteEntryResult {
    val keyName: String

    data class Success(
        override val keyName: String,
        val removed: Boolean,
    ) : StorageWriteEntryResult

    data class Failure(
        override val keyName: String,
        val stage: StorageWriteFailureStage,
        val issue: StorageIssue,
    ) : StorageWriteEntryResult
}

data class StorageBatchResult(
    val owner: StorageOwnerId,
    val entries: List<StorageWriteEntryResult>,
    val syncAttempted: Boolean,
) {
    val isFullySuccessful: Boolean
        get() = entries.all { it is StorageWriteEntryResult.Success }

    fun resultFor(keyName: String): StorageWriteEntryResult? =
        entries.firstOrNull { it.keyName == keyName }
}

data class StorageRefreshResult(
    val refreshedKeys: Set<String>,
    val issues: List<StorageIssue>,
)

data class StorageSnapshotResult(
    val values: Map<String, StoredValue>,
    val issues: List<StorageIssue>,
)

enum class StorageRestoreSkipReason {
    UnknownKey,
    NotBackupEligible,
    TypeMismatch,
    DecodeFailed,
}

data class StorageRestoreSkip(
    val keyName: String,
    val reason: StorageRestoreSkipReason,
    val message: String,
)

data class StorageRestoreResult(
    val batches: Map<StorageOwnerId, StorageBatchResult>,
    val skipped: List<StorageRestoreSkip>,
    val pendingAppLockRestore: Boolean,
) {
    val isFullySuccessful: Boolean
        get() = batches.values.all(StorageBatchResult::isFullySuccessful)
}
