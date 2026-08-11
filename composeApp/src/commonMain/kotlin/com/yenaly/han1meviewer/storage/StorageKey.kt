package com.yenaly.han1meviewer.storage

enum class StorageOwnerId(val mmkvId: String) {
    Auth("han1me.auth"),
    Settings("han1me.settings"),
    UiState("han1me.ui_state"),
    MigrationMeta("han1me.migration_meta"),
}

enum class StorageBackupPolicy {
    Excluded,
    LegacyV1,
}

enum class LegacyStorageSource {
    PackageName,
    DefaultPreferences,
    SettingPreferences,
    None,
}

class StorageKey<T> internal constructor(
    val owner: StorageOwnerId,
    val name: String,
    private val defaultProvider: () -> T,
    val codec: StorageCodec<T>,
    val backupPolicy: StorageBackupPolicy,
    val legacySource: LegacyStorageSource,
) {
    init {
        require(name.isNotBlank()) { "Storage key must not be blank" }
        require(!name.startsWith(RESERVED_PREFIX)) {
            "Storage key '$name' uses the reserved prefix $RESERVED_PREFIX"
        }
    }

    fun defaultValue(): T = codec.copy(defaultProvider())

    override fun toString(): String = "${owner.name}:$name"

    internal companion object {
        const val RESERVED_PREFIX = "__han1me_storage."
    }
}

sealed interface StorageMutation {
    val key: StorageKey<*>

    data class Set<T>(
        override val key: StorageKey<T>,
        val value: T,
    ) : StorageMutation

    data class Remove(
        override val key: StorageKey<*>,
    ) : StorageMutation
}
