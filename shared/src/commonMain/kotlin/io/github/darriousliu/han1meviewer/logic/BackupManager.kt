package io.github.darriousliu.han1meviewer.logic

import io.github.darriousliu.han1meviewer.BuildConfig
import io.github.darriousliu.han1meviewer.Preferences
import io.github.darriousliu.han1meviewer.logic.dao.CheckInRecordDatabase
import io.github.darriousliu.han1meviewer.logic.dao.DownloadDatabase
import io.github.darriousliu.han1meviewer.logic.dao.HistoryDatabase
import io.github.darriousliu.han1meviewer.logic.dao.MiscellanyDatabase
import io.github.darriousliu.han1meviewer.logic.entity.CheckInRecordEntity
import io.github.darriousliu.han1meviewer.logic.entity.HKeyframeEntity
import io.github.darriousliu.han1meviewer.logic.entity.SideDishEntity
import io.github.darriousliu.han1meviewer.logic.entity.WatchHistoryEntity
import io.github.darriousliu.han1meviewer.logic.entity.download.DownloadCategoryEntity
import io.github.darriousliu.han1meviewer.logic.entity.download.DownloadGroupEntity
import io.github.darriousliu.han1meviewer.logic.entity.download.HanimeCategoryCrossRef
import io.github.darriousliu.han1meviewer.logic.entity.download.HanimeDownloadEntity
import io.github.darriousliu.han1meviewer.mmkv.LegacyPreferenceKeys
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.writeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * 设置与数据库的整体备份 / 恢复。
 *
 * 文件的落盘和读取走 FileKit 的 [PlatformFile]，所以整个类不再依赖 Android 的
 * `Context` / `Uri` / `OutputStream`——调用方拿到什么句柄（Android 的 SAF `Uri`、
 * 桌面/iOS 的路径）自己包成 [PlatformFile] 传进来即可。
 */
object BackupManager {
    private const val BACKUP_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    @Serializable
    private data class BackupData(
        val version: Int = BACKUP_VERSION,
        val appVersionCode: Int = BuildConfig.VERSION_CODE,
        val appVersionName: String = BuildConfig.VERSION_NAME,
        val exportedAt: Long = Clock.System.now().toEpochMilliseconds(),
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
        data class BooleanValue(val value: Boolean) : PreferenceValue

        @Serializable
        data class FloatValue(val value: Float) : PreferenceValue

        @Serializable
        data class IntValue(val value: Int) : PreferenceValue

        @Serializable
        data class LongValue(val value: Long) : PreferenceValue

        @Serializable
        data class StringValue(val value: String) : PreferenceValue

        @Serializable
        data class StringSetValue(val value: Set<String>) : PreferenceValue
    }

    suspend fun exportTo(file: PlatformFile) {
        val backup = BackupData(
            settings = exportSettings(),
            hKeyframes = MiscellanyDatabase.instance.hKeyframeDao.getAll(),
            checkInRecords = CheckInRecordDatabase.instance.checkInDao().getAllRecords(),
            sideDishes = CheckInRecordDatabase.instance.sideDishDao().getAll(),
            watchHistories = HistoryDatabase.instance.watchHistory.getAll(),
            downloadGroups = DownloadDatabase.instance.downloadGroupDao.getAllGroupsOnce(),
            downloads = DownloadDatabase.instance.hanimeDownloadDao.getAll(),
            downloadCategories = DownloadDatabase.instance.downloadCategoryDao.getAllCategoriesOnce(),
            downloadCategoryCrossRefs = DownloadDatabase.instance.downloadCategoryDao.getAllCrossRefs(),
        )
        file.writeString(json.encodeToString(backup))
    }

    suspend fun importFrom(file: PlatformFile) {
        val backup = json.decodeFromString<BackupData>(file.readString())

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

        backup.settings?.let { settings ->
            importSettings(settings)
        }
    }

    private fun exportSettings(): Map<String, PreferenceValue> =
        Preferences.exportSettings().mapValues { (_, value) -> value.toPreferenceValue() }

    /**
     * 旧版本导出的备份文件里，key 还是迁移前的下划线形式，先过一遍映射表再写回。
     */
    private fun importSettings(settings: Map<String, PreferenceValue>) {
        Preferences.importSettings(
            settings.entries.associate { (key, value) ->
                (LegacyPreferenceKeys.settings[key] ?: key) to value.raw
            }
        )
    }

    private fun Any.toPreferenceValue(): PreferenceValue = when (this) {
        is Boolean -> PreferenceValue.BooleanValue(this)
        is Float -> PreferenceValue.FloatValue(this)
        is Int -> PreferenceValue.IntValue(this)
        is Long -> PreferenceValue.LongValue(this)
        else -> PreferenceValue.StringValue(toString())
    }

    private val PreferenceValue.raw: Any
        get() = when (this) {
            is PreferenceValue.BooleanValue -> value
            is PreferenceValue.FloatValue -> value
            is PreferenceValue.IntValue -> value
            is PreferenceValue.LongValue -> value
            is PreferenceValue.StringValue -> value
            // 现在没有任何配置项是 Set<String>，留个兜底不至于崩
            is PreferenceValue.StringSetValue -> value.joinToString(",")
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
