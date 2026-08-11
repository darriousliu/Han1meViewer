package com.yenaly.han1meviewer.logic

import android.content.Context
import android.net.Uri
import com.yenaly.han1meviewer.platform.AppBuildInfoProvider
import com.yenaly.han1meviewer.storage.AppStorage
import com.yenaly.han1meviewer.storage.StorageSchema
import com.yenaly.han1meviewer.storage.StorageWriteEntryResult
import com.yenaly.han1meviewer.storage.StoredValue
import com.yenaly.han1meviewer.logic.dao.CheckInRecordDatabase
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.dao.HistoryDatabase
import com.yenaly.han1meviewer.logic.dao.MiscellanyDatabase
import com.yenaly.han1meviewer.logic.entity.CheckInRecordEntity
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.logic.entity.SideDishEntity
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import com.yenaly.han1meviewer.logic.entity.download.DownloadCategoryEntity
import com.yenaly.han1meviewer.logic.entity.download.DownloadGroupEntity
import com.yenaly.han1meviewer.logic.entity.download.HanimeCategoryCrossRef
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

data class BackupImportResult(
    val pendingAppLockRestore: Boolean,
    val skippedSettings: List<String>,
)

object BackupManager {
    private const val BACKUP_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = true
        classDiscriminator = "type"
    }

    private val hasPendingAppLockRestore = AtomicBoolean(false)

    @Serializable
    private data class BackupData(
        val version: Int = BACKUP_VERSION,
        val appVersionCode: Int = AppBuildInfoProvider.current.versionCode,
        val appVersionName: String = AppBuildInfoProvider.current.versionName,
        val exportedAt: Long = System.currentTimeMillis(),
        val settings: Map<String, PreferenceValue>? = null,
        val hKeyframes: List<HKeyframeEntity>? = null,
        val checkInRecords: List<CheckInRecordEntity>? = null,
        val sideDishes: List<SideDishEntity>? = null,
        val watchHistories: List<WatchHistoryEntity>? = null,
        val downloadGroups: List<DownloadGroupEntity>? = null,
        val downloads: List<HanimeDownloadEntity>? = null,
        val downloadCategories: List<DownloadCategoryEntity>? = null,
        val downloadCategoryCrossRefs: List<HanimeCategoryCrossRef>? = null,
    )

    @Serializable
    private sealed interface PreferenceValue {
        @Serializable
        @SerialName("com.yenaly.han1meviewer.logic.BackupManager.PreferenceValue.BooleanValue")
        data class BooleanValue(val value: Boolean) : PreferenceValue

        @Serializable
        @SerialName("com.yenaly.han1meviewer.logic.BackupManager.PreferenceValue.FloatValue")
        data class FloatValue(val value: Float) : PreferenceValue

        @Serializable
        @SerialName("com.yenaly.han1meviewer.logic.BackupManager.PreferenceValue.IntValue")
        data class IntValue(val value: Int) : PreferenceValue

        @Serializable
        @SerialName("com.yenaly.han1meviewer.logic.BackupManager.PreferenceValue.LongValue")
        data class LongValue(val value: Long) : PreferenceValue

        @Serializable
        @SerialName("com.yenaly.han1meviewer.logic.BackupManager.PreferenceValue.StringValue")
        data class StringValue(val value: String) : PreferenceValue

        @Serializable
        @SerialName("com.yenaly.han1meviewer.logic.BackupManager.PreferenceValue.StringSetValue")
        data class StringSetValue(val value: Set<String>) : PreferenceValue
    }

    suspend fun exportTo(context: Context, uri: Uri) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            exportTo(context, outputStream)
        } ?: error("Unable to open backup file")
    }

    suspend fun importFrom(context: Context, uri: Uri): BackupImportResult {
        val backup = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            json.decodeFromString<BackupData>(inputStream.bufferedReader().readText())
        } ?: error("Unable to open backup file")

        backup.hKeyframes?.let { hKeyframes ->
            MiscellanyDatabase.instance.hKeyframeDao.apply {
                deleteAll()
                insertAll(hKeyframes)
            }
        }

        backup.checkInRecords?.let { checkInRecords ->
            CheckInRecordDatabase.instance.checkInDao().apply {
                deleteAll()
                insertAll(checkInRecords.map { it.normalizeSideDishes() })
            }
        }

        backup.sideDishes?.let { sideDishes ->
            CheckInRecordDatabase.instance.sideDishDao().apply {
                deleteAll()
                insertAll(sideDishes)
            }
        }

        backup.watchHistories?.let { watchHistories ->
            HistoryDatabase.instance.watchHistory.apply {
                deleteAll()
                insertAll(watchHistories)
            }
        }

        if (backup.downloadGroups != null || backup.downloads != null ||
            backup.downloadCategories != null || backup.downloadCategoryCrossRefs != null
        ) {
            val downloadGroups = backup.downloadGroups.orEmpty()
            val groupIds = downloadGroups.mapTo(mutableSetOf()) { it.id } +
                    DownloadGroupEntity.DEFAULT_GROUP_ID
            val downloads = backup.downloads.orEmpty().map { download ->
                if (download.groupId in groupIds) {
                    download
                } else {
                    download.copy(groupId = DownloadGroupEntity.DEFAULT_GROUP_ID)
                }
            }
            val downloadCategories = backup.downloadCategories.orEmpty()
            val downloadIds = downloads.mapTo(mutableSetOf()) { it.id }
            val categoryIds = downloadCategories.mapTo(mutableSetOf()) { it.id }
            val crossRefs = backup.downloadCategoryCrossRefs.orEmpty().filter { crossRef ->
                crossRef.videoId in downloadIds && crossRef.categoryId in categoryIds
            }

            DownloadDatabase.instance.apply {
                downloadCategoryDao.deleteAllCrossRefs()
                hanimeDownloadDao.deleteAll()
                downloadCategoryDao.deleteAllCategories()
                downloadGroupDao.deleteAll()
                downloadGroupDao.insertAll(downloadGroups)
                downloadGroupDao.insertDefaultGroup()
                downloadCategoryDao.insertAllCategories(downloadCategories)
                hanimeDownloadDao.insertAll(downloads)
                downloadCategoryDao.insertAllCrossRefs(crossRefs)
            }
        }

        val storageRestore = backup.settings
            ?.mapValues { (_, value) -> value.toStoredValue() }
            ?.let(AppStorage::restoreLegacyV1)

        if (storageRestore != null) {
            check(storageRestore.isFullySuccessful) { "Unable to persist all restored settings" }
        }

        val pendingAppLockRestore = storageRestore?.pendingAppLockRestore == true
        hasPendingAppLockRestore.set(pendingAppLockRestore)

        AppStorage.refreshAll()
        return BackupImportResult(
            pendingAppLockRestore = pendingAppLockRestore,
            skippedSettings = storageRestore?.skipped.orEmpty().map { it.keyName },
        )
    }

    /** Completes a pending backup restore only after the UI has performed device authentication. */
    fun completePendingAppLockRestore(authenticationSucceeded: Boolean): Boolean {
        if (!hasPendingAppLockRestore.compareAndSet(true, false)) return false
        val result = AppStorage.settings.state(StorageSchema.Settings.useLockScreen)
            .set(authenticationSucceeded)
        return authenticationSucceeded && result is StorageWriteEntryResult.Success
    }

    private suspend fun exportTo(context: Context, outputStream: OutputStream) {
        val settingsSnapshot = AppStorage.snapshotLegacyV1()
        check(settingsSnapshot.issues.isEmpty()) { "Unable to read all settings for backup" }
        val backup = BackupData(
            settings = settingsSnapshot.values.mapValues { (_, value) ->
                value.toPreferenceValue()
            },
            hKeyframes = MiscellanyDatabase.instance.hKeyframeDao.getAll(),
            checkInRecords = CheckInRecordDatabase.instance.checkInDao().getAllRecords(),
            sideDishes = CheckInRecordDatabase.instance.sideDishDao().getAll(),
            watchHistories = HistoryDatabase.instance.watchHistory.getAll(),
            downloadGroups = DownloadDatabase.instance.downloadGroupDao.getAllGroupsOnce(),
            downloads = DownloadDatabase.instance.hanimeDownloadDao.getAll(),
            downloadCategories = DownloadDatabase.instance.downloadCategoryDao.getAllCategoriesOnce(),
            downloadCategoryCrossRefs = DownloadDatabase.instance.downloadCategoryDao.getAllCrossRefs(),
        )
        outputStream.bufferedWriter().use { writer ->
            writer.write(json.encodeToString(backup))
        }
    }

    private fun StoredValue.toPreferenceValue(): PreferenceValue = when (this) {
        is StoredValue.BooleanValue -> PreferenceValue.BooleanValue(value)
        is StoredValue.FloatValue -> PreferenceValue.FloatValue(value)
        is StoredValue.IntValue -> PreferenceValue.IntValue(value)
        is StoredValue.LongValue -> PreferenceValue.LongValue(value)
        is StoredValue.StringValue -> PreferenceValue.StringValue(value)
        is StoredValue.StringSetValue -> PreferenceValue.StringSetValue(value.toSet())
        else -> error("Backup version 1 does not support $kind")
    }

    private fun PreferenceValue.toStoredValue(): StoredValue = when (this) {
        is PreferenceValue.BooleanValue -> StoredValue.BooleanValue(value)
        is PreferenceValue.FloatValue -> StoredValue.FloatValue(value)
        is PreferenceValue.IntValue -> StoredValue.IntValue(value)
        is PreferenceValue.LongValue -> StoredValue.LongValue(value)
        is PreferenceValue.StringValue -> StoredValue.StringValue(value)
        is PreferenceValue.StringSetValue -> StoredValue.StringSetValue(value.toSet())
    }

    private fun CheckInRecordEntity.normalizeSideDishes(): CheckInRecordEntity {
        if (sideDishes.isBlank()) return this
        val normalized = sideDishes
            .replace("\\u001E", "\u001E")
            .replace("\\u001e", "\u001E")
            .split(",")
            .joinToString(",") { item ->
                if (item.contains("\u001E") || !item.contains("|")) {
                    item
                } else {
                    val title = item.substringBefore("|")
                    val videoCode = item.substringAfter("|", "")
                    if (videoCode.isBlank()) title else "$title\u001E$videoCode"
                }
            }
        return copy(sideDishes = normalized)
    }
}
