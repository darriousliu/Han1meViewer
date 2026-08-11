package com.yenaly.han1meviewer.logic.dao

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.yenaly.han1meviewer.logic.entity.HanimeAdvancedSearchHistoryEntity
import com.yenaly.han1meviewer.logic.entity.SearchHistoryEntity
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/22 022 22:46
 */
@Database(
    entities = [SearchHistoryEntity::class,
        WatchHistoryEntity::class,
        HanimeAdvancedSearchHistoryEntity::class],
    version = 4, exportSchema = true
)
@ConstructedBy(HistoryDatabaseConstructor::class)
abstract class HistoryDatabase : RoomDatabase() {

    abstract val searchHistory: SearchHistoryDao

    abstract val watchHistory: WatchHistoryDao

    abstract val hanimeAdvancedSearchHistory: HanimeAdvancedSearchHistoryDao

    companion object {
        val instance: HistoryDatabase by lazy {
            createHistoryDatabaseBuilder()
                .addMigrations(
                    Migration1To2,
                    Migration2To3,
                    Migration3To4
                )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
    }

    object Migration1To2 : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.prepare("SELECT id, redirectLink FROM WatchHistoryEntity").use { cursor ->
                connection.prepare(
                    "UPDATE OR REPLACE WatchHistoryEntity SET redirectLink = ? WHERE id = ?"
                ).use { updateStatement ->
                    while (cursor.step()) {
                        val id = cursor.getInt(0)
                        val url = cursor.getText(1)
                        val videoCode =
                            url.substringAfter("v=") // 不用 String.toVideoCode() 的原因是，防止該拓展函數因不可抗力改變導致 migrate 失敗
                        updateStatement.bindText(1, videoCode)
                        updateStatement.bindInt(2, id)
                        updateStatement.step()
                        updateStatement.reset()
                        updateStatement.clearBindings()
                    }
                }
            }
            connection.execSQL(
                """ALTER TABLE WatchHistoryEntity
                   RENAME COLUMN redirectLink TO videoCode"""
            )
        }
    }
    object Migration2To3 : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            // 增加播放进度列，默认值为 0
            connection.execSQL(
                """ALTER TABLE WatchHistoryEntity
                   ADD COLUMN progress INTEGER NOT NULL DEFAULT 0"""
            )
        }
    }
    object Migration3To4 : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `HanimeAdvancedSearchHistory` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `query` TEXT,
                    `genre` TEXT,
                    `sort` TEXT,
                    `broad` INTEGER,
                    `date` TEXT,
                    `duration` TEXT,
                    `tags` TEXT,
                    `brands` TEXT,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }
}

@Suppress("KotlinNoActualForExpect")
expect object HistoryDatabaseConstructor : RoomDatabaseConstructor<HistoryDatabase> {
    override fun initialize(): HistoryDatabase
}
