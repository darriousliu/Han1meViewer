package com.yenaly.han1meviewer.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

internal data class LegacySharedPreferencesMigrationResult(
    val alreadyCompleted: Boolean,
    val migratedKeys: Set<String>,
    val preservedTargetKeys: Set<String>,
    val ignoredInvalidKeys: Set<String>,
)

/**
 * One-shot migration from the three legacy SharedPreferences files.
 *
 * Every key declares its one valid legacy source in [StorageSchema]. Existing MMKV values always
 * win, so a partially completed migration can safely resume without overwriting newer data.
 */
internal object LegacySharedPreferencesMigration {
    private const val SETTING_PREFERENCES_NAME = "setting_pref"

    fun migrate(
        context: Context,
        preparedStorage: PreparedAppStorage,
        issueReporter: StorageIssueReporter,
    ): LegacySharedPreferencesMigrationResult {
        val marker = StorageSchema.MigrationMeta.sharedPreferencesV1
        when (val markerRead = preparedStorage.read(marker)) {
            is StorageBackendReadResult.Value -> {
                if (markerRead.value == StoredValue.BooleanValue(true)) {
                    return LegacySharedPreferencesMigrationResult(
                        alreadyCompleted = true,
                        migratedKeys = emptySet(),
                        preservedTargetKeys = emptySet(),
                        ignoredInvalidKeys = emptySet(),
                    )
                }
            }
            StorageBackendReadResult.Missing -> Unit
            is StorageBackendReadResult.Failure -> {
                issueReporter.report(markerRead.issue)
                throw LegacySharedPreferencesMigrationException(markerRead.issue)
            }
        }

        val sources = try {
            legacySnapshots(context)
        } catch (failure: LegacySharedPreferencesMigrationException) {
            issueReporter.report(failure.issue)
            throw failure
        }
        val migrated = linkedMapOf<StorageKey<*>, StoredValue>()
        val preserved = linkedSetOf<String>()
        val invalid = linkedSetOf<String>()

        StorageSchema.allKeys.forEach { key ->
            val source = sources[key.legacySource] ?: return@forEach
            if (!source.containsKey(key.name)) return@forEach

            if (preparedStorage.containsOrThrow(key, issueReporter)) {
                when (preparedStorage.validateExistingTarget(key, issueReporter)) {
                    ExistingTargetStatus.Valid -> {
                        preserved += key.name
                        return@forEach
                    }
                    ExistingTargetStatus.Invalid -> {
                        invalid += key.name
                        return@forEach
                    }
                    ExistingTargetStatus.Missing -> Unit
                }
            }

            val legacyValue = source[key.name]
            val encodedValue = runCatching {
                legacyValue.toStoredValue(key.codec.storageKind).normalizedFor(key)
            }.getOrElse { cause ->
                issueReporter.report(
                    StorageIssue(
                        owner = key.owner,
                        keyName = key.name,
                        operation = StorageOperation.Read,
                        message = "Ignoring invalid legacy SharedPreferences value: " +
                            "expected ${key.codec.storageKind}, received ${legacyValue?.let { it::class.simpleName }}",
                        cause = cause,
                    ),
                )
                invalid += key.name
                return@forEach
            }

            when (val write = preparedStorage.write(key, encodedValue)) {
                StorageBackendWriteResult.Success -> Unit
                is StorageBackendWriteResult.Failure -> {
                    issueReporter.report(write.issue)
                    rollbackNewValue(preparedStorage, key, issueReporter)
                    throw LegacySharedPreferencesMigrationException(write.issue)
                }
            }

            val verificationIssue = preparedStorage.verify(key, encodedValue)
            if (verificationIssue != null) {
                issueReporter.report(verificationIssue)
                rollbackNewValue(preparedStorage, key, issueReporter)
                throw LegacySharedPreferencesMigrationException(verificationIssue)
            }
            migrated[key] = encodedValue
        }

        migrated.keys.mapTo(linkedSetOf()) { it.owner }.forEach { owner ->
            preparedStorage.sync(owner)?.let { issue ->
                issueReporter.report(issue)
                throw LegacySharedPreferencesMigrationException(issue)
            }
        }

        migrated.forEach { (key, expected) ->
            preparedStorage.verify(key, expected)?.let { issue ->
                issueReporter.report(issue)
                rollbackNewValue(preparedStorage, key, issueReporter)
                throw LegacySharedPreferencesMigrationException(issue)
            }
        }

        val markerValue = StoredValue.BooleanValue(true)
        when (val write = preparedStorage.write(marker, markerValue)) {
            StorageBackendWriteResult.Success -> Unit
            is StorageBackendWriteResult.Failure -> {
                issueReporter.report(write.issue)
                rollbackNewValue(preparedStorage, marker, issueReporter)
                throw LegacySharedPreferencesMigrationException(write.issue)
            }
        }
        preparedStorage.sync(StorageOwnerId.MigrationMeta)?.let { issue ->
            issueReporter.report(issue)
            throw LegacySharedPreferencesMigrationException(issue)
        }
        preparedStorage.verify(marker, markerValue)?.let { issue ->
            issueReporter.report(issue)
            rollbackNewValue(preparedStorage, marker, issueReporter)
            throw LegacySharedPreferencesMigrationException(issue)
        }

        return LegacySharedPreferencesMigrationResult(
            alreadyCompleted = false,
            migratedKeys = migrated.keys.mapTo(linkedSetOf()) { it.name },
            preservedTargetKeys = preserved,
            ignoredInvalidKeys = invalid,
        )
    }

    private fun legacySnapshots(context: Context): Map<LegacyStorageSource, Map<String, *>> = mapOf(
        LegacyStorageSource.PackageName to context
            .getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
            .safeSnapshot(),
        LegacyStorageSource.DefaultPreferences to PreferenceManager
            .getDefaultSharedPreferences(context)
            .safeSnapshot(),
        LegacyStorageSource.SettingPreferences to context
            .getSharedPreferences(SETTING_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .safeSnapshot(),
    )

    private fun SharedPreferences.safeSnapshot(): Map<String, *> = try {
        all.toMap()
    } catch (cause: Throwable) {
        throw LegacySharedPreferencesMigrationException(
            StorageIssue(
                owner = StorageOwnerId.MigrationMeta,
                keyName = null,
                operation = StorageOperation.Read,
                message = "Unable to snapshot legacy SharedPreferences",
                cause = cause,
            ),
        )
    }

    private fun Any?.toStoredValue(expectedKind: StorageValueKind): StoredValue = when (expectedKind) {
        StorageValueKind.Boolean -> StoredValue.BooleanValue(this as Boolean)
        StorageValueKind.Int -> StoredValue.IntValue(this as Int)
        StorageValueKind.Long -> StoredValue.LongValue(this as Long)
        StorageValueKind.Float -> StoredValue.FloatValue(this as Float)
        StorageValueKind.Double -> StoredValue.DoubleValue(this as Double)
        StorageValueKind.String -> StoredValue.StringValue(this as String)
        StorageValueKind.StringSet -> {
            val values = this as Set<*>
            require(values.all { it is String }) { "SharedPreferences set contains a non-String value" }
            StoredValue.StringSetValue(values.filterIsInstance<String>().toSet())
        }
        StorageValueKind.ByteArray,
        StorageValueKind.UInt,
        StorageValueKind.ULong,
        -> error("SharedPreferences cannot contain $expectedKind")
    }

    @Suppress("UNCHECKED_CAST")
    private fun StoredValue.normalizedFor(key: StorageKey<*>): StoredValue {
        val codec = key.codec as StorageCodec<Any?>
        val decoded = codec.decode(this)
        return requireNotNull(codec.encode(decoded)) {
            "Legacy non-null value unexpectedly encoded as removal"
        }
    }

    private fun PreparedAppStorage.verify(
        key: StorageKey<*>,
        expected: StoredValue,
    ): StorageIssue? = when (val actual = read(key)) {
        is StorageBackendReadResult.Value -> if (actual.value == expected) {
            null
        } else {
            verificationIssue(key, "Persisted value differs from the migrated value")
        }
        StorageBackendReadResult.Missing -> verificationIssue(key, "Persisted value is missing after migration")
        is StorageBackendReadResult.Failure -> actual.issue
    }

    private fun verificationIssue(key: StorageKey<*>, message: String) = StorageIssue(
        owner = key.owner,
        keyName = key.name,
        operation = StorageOperation.Read,
        message = message,
    )

    private fun PreparedAppStorage.containsOrThrow(
        key: StorageKey<*>,
        issueReporter: StorageIssueReporter,
    ): Boolean = try {
        contains(key)
    } catch (cause: Throwable) {
        val issue = StorageIssue(
            owner = key.owner,
            keyName = key.name,
            operation = StorageOperation.Read,
            message = "Unable to check whether the MMKV target already exists",
            cause = cause,
        )
        issueReporter.report(issue)
        throw LegacySharedPreferencesMigrationException(issue)
    }

    /** Existing targets win even when corrupt; Store decoding will safely fall back to defaults. */
    private fun PreparedAppStorage.validateExistingTarget(
        key: StorageKey<*>,
        issueReporter: StorageIssueReporter,
    ): ExistingTargetStatus = when (val existing = read(key)) {
        is StorageBackendReadResult.Value -> runCatching {
            val normalized = existing.value.normalizedFor(key)
            require(normalized == existing.value) { "Existing MMKV value is not canonical" }
        }.fold(
            onSuccess = { ExistingTargetStatus.Valid },
            onFailure = { cause ->
                issueReporter.report(
                    StorageIssue(
                        owner = key.owner,
                        keyName = key.name,
                        operation = StorageOperation.Read,
                        message = "Existing MMKV value is invalid; preserving it and using the default",
                        cause = cause,
                    ),
                )
                ExistingTargetStatus.Invalid
            },
        )
        StorageBackendReadResult.Missing -> ExistingTargetStatus.Missing
        is StorageBackendReadResult.Failure -> {
            issueReporter.report(existing.issue)
            throw LegacySharedPreferencesMigrationException(existing.issue)
        }
    }

    private enum class ExistingTargetStatus {
        Valid,
        Invalid,
        Missing,
    }

    private fun rollbackNewValue(
        preparedStorage: PreparedAppStorage,
        key: StorageKey<*>,
        issueReporter: StorageIssueReporter,
    ) {
        when (val rollback = preparedStorage.write(key, null)) {
            StorageBackendWriteResult.Success -> Unit
            is StorageBackendWriteResult.Failure -> issueReporter.report(rollback.issue)
        }
        preparedStorage.sync(key.owner)?.let(issueReporter::report)
    }
}

internal class LegacySharedPreferencesMigrationException(
    val issue: StorageIssue,
) : IllegalStateException(
    buildString {
        append("Legacy SharedPreferences migration failed for ")
        append(issue.owner)
        issue.keyName?.let { append(':').append(it) }
        append(": ").append(issue.message)
    },
    issue.cause,
)
