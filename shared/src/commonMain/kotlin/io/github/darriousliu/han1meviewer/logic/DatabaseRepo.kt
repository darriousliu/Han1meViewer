package io.github.darriousliu.han1meviewer.logic

import co.touchlab.kermit.Logger
import io.github.darriousliu.han1meviewer.HJson
import io.github.darriousliu.han1meviewer.Preferences
import io.github.darriousliu.han1meviewer.logic.dao.DownloadDatabase
import io.github.darriousliu.han1meviewer.logic.dao.HistoryDatabase
import io.github.darriousliu.han1meviewer.logic.dao.MiscellanyDatabase
import io.github.darriousliu.han1meviewer.logic.entity.HKeyframeEntity
import io.github.darriousliu.han1meviewer.logic.entity.HKeyframeHeader
import io.github.darriousliu.han1meviewer.logic.entity.HKeyframeType
import io.github.darriousliu.han1meviewer.logic.entity.HanimeAdvancedSearchHistoryEntity
import io.github.darriousliu.han1meviewer.logic.entity.SearchHistoryEntity
import io.github.darriousliu.han1meviewer.logic.entity.WatchHistoryEntity
import io.github.darriousliu.han1meviewer.logic.entity.download.DownloadGroupEntity
import io.github.darriousliu.han1meviewer.logic.entity.download.HanimeDownloadEntity
import io.github.darriousliu.han1meviewer.logic.model.SearchOption
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.SHARED_H_KEYFRAME_CODES
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.ExperimentalSerializationApi
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/22 022 23:00
 */
object DatabaseRepo {

    object HKeyframe {
        private val hKeyframeDao = MiscellanyDatabase.instance.hKeyframeDao

        fun loadAll(keyword: String? = null) =
            if (keyword != null) hKeyframeDao.loadAll(keyword)
            else hKeyframeDao.loadAll()

        // #issue-106: 剧集分类
        //
        // 迁移前是 `assets.list("h_keyframes")` 枚举目录，compose-resources 没有这个能力，
        // 改成读构建期生成的 [SHARED_H_KEYFRAME_CODES]（见 :core:resource:generateSharedHKeyframeIndex）。
        // 文件布局仍然是一个视频一个 json，贡献方式不变。
        @OptIn(ExperimentalResourceApi::class)
        fun loadAllShared(): Flow<List<HKeyframeType>> = flow {
            val res = SHARED_H_KEYFRAME_CODES
                .mapNotNull { videoCode -> readSharedHKeyframe(videoCode) }
                .sortedWith(compareBy<HKeyframeEntity> { it.group }.thenBy { it.episode })
                .groupBy { it.group ?: "???" }
                .flatMap { (group, entities) ->
                    listOf(HKeyframeHeader(title = group, attached = entities)) + entities
                }
            emit(res)
        }

        /** 读打包在 composeResources 里的共享关键 H 帧，读不到或解析失败返回 null。 */
        @OptIn(ExperimentalResourceApi::class)
        private suspend fun readSharedHKeyframe(videoCode: String): HKeyframeEntity? =
            runCatching {
                HJson.decodeFromString<HKeyframeEntity>(
                    Res.readBytes("files/h_keyframes/$videoCode.json").decodeToString()
                )
            }.onFailure { e ->
                Logger.e(tag = "HKeyframe") { "读取关键帧失败: $videoCode.json, ${e.message}" }
            }.getOrNull()

        suspend fun findBy(videoCode: String) =
            hKeyframeDao.findBy(videoCode)

        @OptIn(ExperimentalSerializationApi::class)
        fun observe(videoCode: String): Flow<HKeyframeEntity?> {
            if (Preferences.sharedHKeyframesEnable) {
                return flow t@{
                    val find = hKeyframeDao.findBy(videoCode)
                    if (find == null || Preferences.sharedHKeyframesUseFirst) {
                        // 迁移前靠 FileNotFoundException 判断「没有这个共享帧」，
                        // Res.readBytes 抛的是别的类型，改成先查索引。
                        if (videoCode !in SHARED_H_KEYFRAME_CODES) {
                            Logger.w(tag = "HKeyframe") { "未找到关键帧文件: $videoCode.json" }
                        } else {
                            readSharedHKeyframe(videoCode)?.let { this@t.emit(it) }
                        }
                    } else {
                        hKeyframeDao.observe(videoCode).collect {
                            this@t.emit(it)
                        }
                    }
                }.catch t@{ e ->
                    e.printStackTrace()
                    hKeyframeDao.observe(videoCode).collect {
                        this@t.emit(it)
                    }
                }
            }
            return hKeyframeDao.observe(videoCode)
        }

        suspend fun insert(entity: HKeyframeEntity) = hKeyframeDao.insert(entity)

        suspend fun update(entity: HKeyframeEntity) = hKeyframeDao.update(entity)

        suspend fun delete(entity: HKeyframeEntity) =
            hKeyframeDao.delete(entity)

        suspend fun modifyKeyframe(
            videoCode: String,
            oldKeyframe: HKeyframeEntity.Keyframe, keyframe: HKeyframeEntity.Keyframe,
        ) = hKeyframeDao.modifyKeyframe(videoCode, oldKeyframe, keyframe)

        suspend fun appendKeyframe(
            videoCode: String, title: String,
            keyframe: HKeyframeEntity.Keyframe,
        ) = hKeyframeDao.appendKeyframe(videoCode, title, keyframe)

        suspend fun removeKeyframe(
            videoCode: String,
            keyframe: HKeyframeEntity.Keyframe,
        ) = hKeyframeDao.removeKeyframe(videoCode, keyframe)
    }

    object SearchHistory {
        private val searchHistoryDao = HistoryDatabase.instance.searchHistory

        fun loadAll(keyword: String? = null) =
            if (keyword.isNullOrBlank()) searchHistoryDao.loadAll()
            else searchHistoryDao.loadAll(keyword)

        suspend fun delete(history: SearchHistoryEntity) =
            searchHistoryDao.delete(history)

        suspend fun insert(history: SearchHistoryEntity) =
            searchHistoryDao.insertOrUpdate(history)

        suspend fun deleteByKeyword(query: String) =
            searchHistoryDao.deleteByKeyword(query)
    }

    object HanimeAdvancedSearchRepo {
        private val dao = HistoryDatabase.instance.hanimeAdvancedSearchHistory

        suspend fun saveSearch(
            query: String?,
            genre: String?,
            sort: String?,
            broad: Boolean?,
            date: String?,
            duration: String?,
            tags: Set<SearchOption>?,
            brands: Set<SearchOption>?
        ) {
            val entity = HanimeAdvancedSearchHistoryEntity(
                query = query,
                genre = genre,
                sort = sort,
                broad = broad,
                date = date,
                duration = duration,
                tags = tags?.toDbString(),
                brands = brands?.toDbString()
            )
            dao.insertHistory(entity)
        }

        fun getSearchHistories(limit: Int = 20) = dao.loadHistories(limit)
        suspend fun deleteHistory(id: Long) = dao.deleteHistory(id)
        fun Set<SearchOption>.toDbString(): String =
            mapNotNull { it.searchKey }.joinToString(",")
        fun String.toSearchOptionSet(): Set<SearchOption> =
            if (isBlank()) emptySet()
            else split(",").map { SearchOption(searchKey = it) }.toSet()
    }

    object WatchHistory {
        private val watchHistoryDao = HistoryDatabase.instance.watchHistory

        fun loadAll() =
            watchHistoryDao.loadAll()

        suspend fun delete(history: WatchHistoryEntity) =
            watchHistoryDao.delete(history)

        suspend fun deleteAll() =
            watchHistoryDao.deleteAll()

        suspend fun update(history: WatchHistoryEntity) =
            watchHistoryDao.update(history)

        suspend fun updateProgress(videoCode: String,progress: Long) =
            watchHistoryDao.updateProgress(videoCode, progress)

        suspend fun insert(history: WatchHistoryEntity) =
            watchHistoryDao.insertOrUpdate(history)

        suspend fun findBy(videoCode: String) =
            watchHistoryDao.findBy(videoCode)

        suspend fun getWatched(resultList: List<String>) =
            watchHistoryDao.getWatchedCodes(resultList)
    }

    object HanimeDownload {
        private val hanimeDownloadDao = DownloadDatabase.instance.hanimeDownloadDao
        private val downloadGroupDao = DownloadDatabase.instance.downloadGroupDao
        fun loadAllDownloadingHanime() =
            hanimeDownloadDao.loadAllDownloadingHanime()

        /**
         * 查询所有视频，并且每个视频要有当前他在的分类
         */
        fun loadAllDownloadedHanime(
            sortedBy: HanimeDownloadEntity.SortedBy,
            ascending: Boolean,
        ) = when (sortedBy) {
            HanimeDownloadEntity.SortedBy.TITLE ->
                hanimeDownloadDao.loadAllDownloadedHanimeByTitle(ascending)

            HanimeDownloadEntity.SortedBy.ID ->
                hanimeDownloadDao.loadAllDownloadedHanimeById(ascending)
        }
        suspend fun delete(videoCode: String, quality: String) =
            hanimeDownloadDao.delete(videoCode, quality)

        suspend fun delete(videoCode: String) =
            hanimeDownloadDao.delete(videoCode)

        suspend fun pauseAll() =
            hanimeDownloadDao.pauseAll()

        suspend fun delete(entity: HanimeDownloadEntity) =
            hanimeDownloadDao.delete(entity)

        suspend fun insert(entity: HanimeDownloadEntity) =
            hanimeDownloadDao.insert(entity)

        suspend fun update(entity: HanimeDownloadEntity) =
            hanimeDownloadDao.update(entity)

        suspend fun find(videoCode: String, quality: String) =
            hanimeDownloadDao.find(videoCode, quality)

        suspend fun find(videoCode: String) =
            hanimeDownloadDao.find(videoCode)

        suspend fun insertDefaultGroup() =
            downloadGroupDao.insertDefaultGroup()

        fun getAllGroups()=
            downloadGroupDao.getAllGroups()

        suspend fun getGroupById(id: Int)=
            downloadGroupDao.getGroupById(id)

        suspend fun updateVideoGroup(videoCode: String, newGroupId: Int)=
            hanimeDownloadDao.updateVideoGroup(videoCode, newGroupId)

        suspend fun createNewGroup(name: String): Long{
            val maxIndex = downloadGroupDao.getMaxOrderIndex() ?: 0
            val newIndex = maxIndex + 1
            val newGroup = DownloadGroupEntity(
                name = name,
                orderIndex = newIndex
            )
            return downloadGroupDao.insert(newGroup)
        }
        suspend fun deleteGroup(group: DownloadGroupEntity) {
            downloadGroupDao.deleteGroup(group)
        }

        suspend fun updateGroup(group: DownloadGroupEntity)=
            downloadGroupDao.update(group)
    }
}