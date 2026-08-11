package com.yenaly.han1meviewer.storage

import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock

open class StorageOwner internal constructor(
    val ownerId: StorageOwnerId,
    keys: List<StorageKey<*>>,
    private val backend: MmkvStorageBackend,
    private val issueReporter: StorageIssueReporter,
) {
    private val writerLock = reentrantLock()
    private val keysByName: Map<String, StorageKey<*>> = keys.associateBy(StorageKey<*>::name)
    private val statesByName: Map<String, StoredState<*>> = keys.associate { key ->
        key.name to createState(key)
    }

    init {
        require(backend.owner == ownerId) { "Backend ${backend.owner} does not match $ownerId" }
        require(keys.all { it.owner == ownerId }) { "Owner $ownerId received a key from another owner" }
        require(keysByName.size == keys.size) { "Owner $ownerId contains duplicate key names" }
    }

    fun findKey(name: String): StorageKey<*>? = keysByName[name]

    fun requireKey(name: String): StorageKey<*> =
        requireNotNull(findKey(name)) { "Unknown storage key ${ownerId.name}:$name" }

    fun <T> state(key: StorageKey<T>): StoredState<T> {
        requireRegistered(key)
        @Suppress("UNCHECKED_CAST")
        return statesByName.getValue(key.name) as StoredState<T>
    }

    fun <T> value(key: StorageKey<T>): T = state(key).value

    /**
     * Applies all mutations under one reentrant owner lock and performs one final MMKV sync.
     * A state is published only after its own write and the final sync both succeed.
     */
    fun writeBatch(mutations: List<StorageMutation>): StorageBatchResult = writerLock.withLock {
        writeBatchLocked(mutations)
    }

    fun refreshAll(): StorageRefreshResult = writerLock.withLock {
        refreshLocked(keysByName.values)
    }

    internal fun refresh(key: StorageKey<*>): StorageRefreshResult = writerLock.withLock {
        requireRegistered(key)
        refreshLocked(listOf(key))
    }

    internal fun <T> update(
        state: StoredState<T>,
        transform: (T) -> T,
    ): StorageWriteEntryResult = writerLock.withLock {
        require(state === state(state.key)) { "StoredState is not owned by $ownerId" }
        writeBatchLocked(listOf(StorageMutation.Set(state.key, transform(state.value))))
            .resultFor(state.key.name)
            ?: error("Missing update result for ${state.key.name}")
    }

    internal fun snapshot(policy: StorageBackupPolicy): StorageSnapshotResult = writerLock.withLock {
        val values = linkedMapOf<String, StoredValue>()
        val issues = mutableListOf<StorageIssue>()
        keysByName.values.filter { it.backupPolicy == policy }.forEach { key ->
            when (val read = backend.read(key)) {
                StorageBackendReadResult.Missing -> Unit
                is StorageBackendReadResult.Failure -> {
                    issues += read.issue.asOperation(StorageOperation.Snapshot)
                }
                is StorageBackendReadResult.Value -> {
                    val validationIssue = validateDecodedValue(key, read.value, StorageOperation.Snapshot)
                    if (validationIssue == null) {
                        values[key.name] = read.value.copyForCaller()
                    } else {
                        issues += validationIssue
                    }
                }
            }
        }
        issues.forEach(::report)
        StorageSnapshotResult(values, issues)
    }

    private fun writeBatchLocked(mutations: List<StorageMutation>): StorageBatchResult {
        if (mutations.isEmpty()) {
            return StorageBatchResult(ownerId, emptyList(), syncAttempted = false)
        }

        val results = MutableList<StorageWriteEntryResult?>(mutations.size) { null }
        val duplicateNames = mutations.groupingBy { it.key.name }.eachCount()
            .filterValues { it > 1 }
            .keys
        val prepared = mutableListOf<PreparedMutation>()

        mutations.forEachIndexed { index, mutation ->
            val key = mutation.key
            val validationMessage = when {
                key.owner != ownerId -> "Key belongs to ${key.owner}, not $ownerId"
                keysByName[key.name] !== key -> "Key is not the registered ${ownerId.name} instance"
                key.name in duplicateNames -> "A batch may mutate a key only once"
                else -> null
            }
            if (validationMessage != null) {
                results[index] = failure(
                    key = key,
                    stage = StorageWriteFailureStage.Validation,
                    operation = StorageOperation.Write,
                    message = validationMessage,
                )
                return@forEachIndexed
            }

            try {
                prepared += prepareMutation(index, mutation)
            } catch (exception: Exception) {
                results[index] = failure(
                    key = key,
                    stage = StorageWriteFailureStage.Encode,
                    operation = StorageOperation.Encode,
                    message = "Unable to encode value with ${key.codec.id}",
                    cause = exception,
                )
            }
        }

        val written = mutableListOf<PreparedMutation>()
        prepared.forEach { item ->
            when (val write = backend.write(item.mutation.key, item.encodedValue)) {
                StorageBackendWriteResult.Success -> written += item
                is StorageBackendWriteResult.Failure -> {
                    report(write.issue)
                    results[item.index] = StorageWriteEntryResult.Failure(
                        keyName = item.mutation.key.name,
                        stage = StorageWriteFailureStage.Write,
                        issue = write.issue,
                    )
                }
            }
        }

        val syncIssue = if (written.isNotEmpty()) backend.sync() else null
        if (syncIssue != null) {
            report(syncIssue)
            written.forEach { item ->
                results[item.index] = StorageWriteEntryResult.Failure(
                    keyName = item.mutation.key.name,
                    stage = StorageWriteFailureStage.Sync,
                    issue = syncIssue.copy(keyName = item.mutation.key.name),
                )
            }
        } else {
            written.forEach { item ->
                publish(item)
                results[item.index] = StorageWriteEntryResult.Success(
                    keyName = item.mutation.key.name,
                    removed = item.mutation is StorageMutation.Remove || item.encodedValue == null,
                )
            }
        }

        return StorageBatchResult(
            owner = ownerId,
            entries = results.mapIndexed { index, result ->
                result ?: error("Missing batch result at index $index")
            },
            syncAttempted = written.isNotEmpty(),
        )
    }

    private fun refreshLocked(keys: Collection<StorageKey<*>>): StorageRefreshResult {
        val refreshed = linkedSetOf<String>()
        val issues = mutableListOf<StorageIssue>()
        keys.forEach { key ->
            val decoded = decodeOrDefault(key, StorageOperation.Refresh)
            decoded.issue?.let(issues::add)
            publishValue(key, decoded.value)
            refreshed += key.name
        }
        issues.forEach(::report)
        return StorageRefreshResult(refreshed, issues)
    }

    private fun createState(key: StorageKey<*>): StoredState<*> = createTypedState(key)

    private fun <T> createTypedState(key: StorageKey<T>): StoredState<T> {
        val decoded = decodeOrDefault(key, StorageOperation.Read)
        decoded.issue?.let(::report)
        return StoredState(key, this, decoded.value)
    }

    private fun <T> decodeOrDefault(
        key: StorageKey<T>,
        operation: StorageOperation,
    ): DecodedValue<T> = when (val read = backend.read(key)) {
        StorageBackendReadResult.Missing -> DecodedValue(key.defaultValue())
        is StorageBackendReadResult.Failure -> DecodedValue(
            value = key.defaultValue(),
            issue = read.issue.asOperation(operation),
        )
        is StorageBackendReadResult.Value -> try {
            DecodedValue(key.codec.decode(read.value))
        } catch (exception: Exception) {
            DecodedValue(
                value = key.defaultValue(),
                issue = StorageIssue(
                    owner = ownerId,
                    keyName = key.name,
                    operation = operation,
                    message = "Stored value is invalid for codec ${key.codec.id}; using default",
                    cause = exception,
                ),
            )
        }
    }

    private fun validateDecodedValue(
        key: StorageKey<*>,
        value: StoredValue,
        operation: StorageOperation,
    ): StorageIssue? = try {
        decodeUnchecked(key, value)
        null
    } catch (exception: Exception) {
        StorageIssue(
            owner = ownerId,
            keyName = key.name,
            operation = operation,
            message = "Stored value is invalid for codec ${key.codec.id}",
            cause = exception,
        )
    }

    private fun prepareMutation(index: Int, mutation: StorageMutation): PreparedMutation =
        when (mutation) {
            is StorageMutation.Remove -> PreparedMutation(
                index = index,
                mutation = mutation,
                encodedValue = null,
                publishedValue = mutation.key.defaultValue(),
            )
            is StorageMutation.Set<*> -> prepareSet(index, mutation)
        }

    private fun <T> prepareSet(
        index: Int,
        mutation: StorageMutation.Set<T>,
    ): PreparedMutation {
        val encoded = mutation.key.codec.encode(mutation.value)
        val canonicalValue = encoded?.let(mutation.key.codec::decode) ?: mutation.value
        return PreparedMutation(index, mutation, encoded, canonicalValue)
    }

    private fun publish(item: PreparedMutation) {
        publishValue(item.mutation.key, item.publishedValue)
    }

    private fun publishValue(key: StorageKey<*>, value: Any?) {
        @Suppress("UNCHECKED_CAST")
        (statesByName.getValue(key.name) as StoredState<Any?>).publish(value)
    }

    private fun decodeUnchecked(key: StorageKey<*>, value: StoredValue): Any? {
        @Suppress("UNCHECKED_CAST")
        return (key.codec as StorageCodec<Any?>).decode(value)
    }

    private fun requireRegistered(key: StorageKey<*>) {
        require(key.owner == ownerId && keysByName[key.name] === key) {
            "Key ${key.name} is not registered with $ownerId"
        }
    }

    private fun failure(
        key: StorageKey<*>,
        stage: StorageWriteFailureStage,
        operation: StorageOperation,
        message: String,
        cause: Throwable? = null,
    ): StorageWriteEntryResult.Failure {
        val issue = StorageIssue(ownerId, key.name, operation, message, cause)
        report(issue)
        return StorageWriteEntryResult.Failure(key.name, stage, issue)
    }

    private fun report(issue: StorageIssue) {
        runCatching { issueReporter.report(issue) }
    }

    private data class DecodedValue<T>(
        val value: T,
        val issue: StorageIssue? = null,
    )

    private data class PreparedMutation(
        val index: Int,
        val mutation: StorageMutation,
        val encodedValue: StoredValue?,
        val publishedValue: Any?,
    )
}

class AuthStore internal constructor(
    backend: MmkvStorageBackend,
    issueReporter: StorageIssueReporter,
) : StorageOwner(StorageOwnerId.Auth, StorageSchema.Auth.keys, backend, issueReporter)

class SettingsStore internal constructor(
    backend: MmkvStorageBackend,
    issueReporter: StorageIssueReporter,
) : StorageOwner(StorageOwnerId.Settings, StorageSchema.Settings.keys, backend, issueReporter)

class UiStateStore internal constructor(
    backend: MmkvStorageBackend,
    issueReporter: StorageIssueReporter,
) : StorageOwner(StorageOwnerId.UiState, StorageSchema.UiState.keys, backend, issueReporter)

class MigrationMetaStore internal constructor(
    backend: MmkvStorageBackend,
    issueReporter: StorageIssueReporter,
) : StorageOwner(StorageOwnerId.MigrationMeta, StorageSchema.MigrationMeta.keys, backend, issueReporter)

private fun StorageIssue.asOperation(operation: StorageOperation): StorageIssue =
    if (this.operation == operation) this else copy(operation = operation)

private fun StoredValue.copyForCaller(): StoredValue = when (this) {
    is StoredValue.ByteArrayValue -> StoredValue.ByteArrayValue(value)
    is StoredValue.StringSetValue -> StoredValue.StringSetValue(value)
    else -> this
}
