package com.yenaly.han1meviewer.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Read-only observable state. Every mutation is serialized and persisted by its owner. */
class StoredState<T> internal constructor(
    val key: StorageKey<T>,
    private val owner: StorageOwner,
    initialValue: T,
) {
    private val mutableFlow = MutableStateFlow(key.codec.copy(initialValue))

    val flow: StateFlow<T> = mutableFlow.asStateFlow()

    val value: T
        get() = key.codec.copy(mutableFlow.value)

    fun set(value: T): StorageWriteEntryResult =
        owner.writeBatch(listOf(StorageMutation.Set(key, value))).resultFor(key.name)
            ?: error("Missing write result for ${key.name}")

    fun update(transform: (T) -> T): StorageWriteEntryResult =
        owner.update(this, transform)

    fun remove(): StorageWriteEntryResult =
        owner.writeBatch(listOf(StorageMutation.Remove(key))).resultFor(key.name)
            ?: error("Missing remove result for ${key.name}")

    fun refresh(): StorageRefreshResult = owner.refresh(key)

    internal fun publish(value: T) {
        val copiedValue = key.codec.copy(value)
        if (!key.codec.equivalent(mutableFlow.value, copiedValue)) {
            mutableFlow.value = copiedValue
        }
    }
}
