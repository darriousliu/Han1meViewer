package com.yenaly.han1meviewer.logic.dao

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * 这是各种 有数据库需求的小功能 的聚集地，
 * 如果这个功能需要数据库就放到这里。
 *
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2023/11/12 012 12:28
 */
@Database(
    entities = [HKeyframeEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(MiscellanyDatabaseConstructor::class)
abstract class MiscellanyDatabase : RoomDatabase() {

    abstract val hKeyframeDao: HKeyframeDao

    companion object {
        val instance: MiscellanyDatabase by lazy {
            createMiscellanyDatabaseBuilder()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
    }
}

@Suppress("KotlinNoActualForExpect")
expect object MiscellanyDatabaseConstructor : RoomDatabaseConstructor<MiscellanyDatabase> {
    override fun initialize(): MiscellanyDatabase
}
