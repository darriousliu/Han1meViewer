package com.yenaly.han1meviewer.storage

import com.ctrip.flight.mmkv.MMKVMode
import com.ctrip.flight.mmkv.MMKV_KMP
import com.ctrip.flight.mmkv.mmkvWithID

internal sealed interface StorageBackendReadResult {
    data object Missing : StorageBackendReadResult
    data class Value(val value: StoredValue) : StorageBackendReadResult
    data class Failure(val issue: StorageIssue) : StorageBackendReadResult
}

internal sealed interface StorageBackendWriteResult {
    data object Success : StorageBackendWriteResult
    data class Failure(val issue: StorageIssue) : StorageBackendWriteResult
}

internal class MmkvStorageBackend(
    val owner: StorageOwnerId,
    private val mmkv: MMKV_KMP,
) {
    fun contains(key: StorageKey<*>): Boolean {
        requireRegisteredOwner(key)
        return mmkv.containsKey(key.name)
    }

    fun read(key: StorageKey<*>): StorageBackendReadResult {
        requireRegisteredOwner(key)
        return try {
            if (!mmkv.containsKey(key.name)) {
                StorageBackendReadResult.Missing
            } else {
                StorageBackendReadResult.Value(
                    when (key.codec.storageKind) {
                        StorageValueKind.Boolean -> StoredValue.BooleanValue(mmkv.getBoolean(key.name))
                        StorageValueKind.Int -> StoredValue.IntValue(mmkv.getInt(key.name))
                        StorageValueKind.Long -> StoredValue.LongValue(mmkv.getLong(key.name))
                        StorageValueKind.Float -> StoredValue.FloatValue(mmkv.getFloat(key.name))
                        StorageValueKind.Double -> StoredValue.DoubleValue(mmkv.getDouble(key.name))
                        StorageValueKind.String -> StoredValue.StringValue(mmkv.getString(key.name))
                        StorageValueKind.StringSet -> StoredValue.StringSetValue(
                            mmkv.getStringSet(key.name).orEmpty().toSet(),
                        )
                        StorageValueKind.ByteArray -> StoredValue.ByteArrayValue(
                            @Suppress("DEPRECATION")
                            (mmkv.takeByteArray(key.name, null) ?: byteArrayOf()),
                        )
                        StorageValueKind.UInt -> StoredValue.UIntValue(mmkv.getUInt(key.name))
                        StorageValueKind.ULong -> StoredValue.ULongValue(mmkv.getULong(key.name))
                    },
                )
            }
        } catch (exception: Exception) {
            StorageBackendReadResult.Failure(
                issue(
                    key = key,
                    operation = StorageOperation.Read,
                    message = "Unable to read persisted value",
                    cause = exception,
                ),
            )
        }
    }

    fun write(key: StorageKey<*>, value: StoredValue?): StorageBackendWriteResult {
        requireRegisteredOwner(key)
        if (value != null && value.kind != key.codec.storageKind) {
            return StorageBackendWriteResult.Failure(
                issue(
                    key = key,
                    operation = StorageOperation.Write,
                    message = "Expected ${key.codec.storageKind} but received ${value.kind}",
                ),
            )
        }
        return try {
            val success = when (value) {
                null -> {
                    mmkv.removeValueForKey(key.name)
                    !mmkv.containsKey(key.name)
                }
                is StoredValue.BooleanValue -> mmkv.set(key.name, value.value)
                is StoredValue.IntValue -> mmkv.set(key.name, value.value)
                is StoredValue.LongValue -> mmkv.set(key.name, value.value)
                is StoredValue.FloatValue -> mmkv.set(key.name, value.value)
                is StoredValue.DoubleValue -> mmkv.set(key.name, value.value)
                is StoredValue.StringValue -> mmkv.set(key.name, value.value)
                is StoredValue.StringSetValue -> mmkv.set(key.name, value.value.toSet())
                is StoredValue.ByteArrayValue -> mmkv.set(key.name, value.value.copyOf())
                is StoredValue.UIntValue -> mmkv.set(key.name, value.value)
                is StoredValue.ULongValue -> mmkv.set(key.name, value.value)
            }
            if (success) {
                StorageBackendWriteResult.Success
            } else {
                StorageBackendWriteResult.Failure(
                    issue(key, StorageOperation.Write, "MMKV rejected the value"),
                )
            }
        } catch (exception: Exception) {
            StorageBackendWriteResult.Failure(
                issue(
                    key = key,
                    operation = StorageOperation.Write,
                    message = "Unable to persist value",
                    cause = exception,
                ),
            )
        }
    }

    fun sync(): StorageIssue? = try {
        mmkv.sync()
        null
    } catch (exception: Exception) {
        issue(
            key = null,
            operation = StorageOperation.Sync,
            message = "Unable to sync MMKV owner ${owner.name}",
            cause = exception,
        )
    }

    fun close() = mmkv.close()

    private fun requireRegisteredOwner(key: StorageKey<*>) {
        require(key.owner == owner) {
            "Key ${key.name} belongs to ${key.owner}, not $owner"
        }
        require(StorageSchema.findKey(owner, key.name) === key) {
            "Key ${key.name} is not the registered ${owner.name} key instance"
        }
    }

    private fun issue(
        key: StorageKey<*>?,
        operation: StorageOperation,
        message: String,
        cause: Throwable? = null,
    ) = StorageIssue(owner, key?.name, operation, message, cause)
}

/** Four process-lifetime MMKV handles. This is the only place that opens them. */
internal class PreparedAppStorage private constructor(
    private val backends: Map<StorageOwnerId, MmkvStorageBackend>,
) {
    fun contains(key: StorageKey<*>): Boolean = backend(key.owner).contains(key)

    fun read(key: StorageKey<*>): StorageBackendReadResult = backend(key.owner).read(key)

    fun write(key: StorageKey<*>, value: StoredValue?): StorageBackendWriteResult =
        backend(key.owner).write(key, value)

    fun sync(owner: StorageOwnerId): StorageIssue? = backend(owner).sync()

    fun syncAll(): List<StorageIssue> = StorageOwnerId.entries.mapNotNull(::sync)

    fun backend(owner: StorageOwnerId): MmkvStorageBackend =
        requireNotNull(backends[owner]) { "Missing backend for $owner" }

    fun close() = backends.values.forEach(MmkvStorageBackend::close)

    companion object {
        fun open(): PreparedAppStorage {
            val opened = mutableListOf<MmkvStorageBackend>()
            return try {
                StorageOwnerId.entries.forEach { owner ->
                    opened += MmkvStorageBackend(
                        owner = owner,
                        mmkv = mmkvWithID(
                            mmapId = owner.mmkvId,
                            mode = MMKVMode.SINGLE_PROCESS,
                        ),
                    )
                }
                PreparedAppStorage(opened.associateBy(MmkvStorageBackend::owner))
            } catch (throwable: Throwable) {
                opened.forEach { runCatching { it.close() } }
                throw throwable
            }
        }
    }
}
