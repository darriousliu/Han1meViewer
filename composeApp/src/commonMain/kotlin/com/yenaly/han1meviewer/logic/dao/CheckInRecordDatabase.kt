package com.yenaly.han1meviewer.logic.dao

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.yenaly.han1meviewer.logic.entity.CheckInRecordEntity
import com.yenaly.han1meviewer.logic.entity.SideDishEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [CheckInRecordEntity::class, SideDishEntity::class],
    version = 4,
    exportSchema = true
)
@ConstructedBy(CheckInRecordDatabaseConstructor::class)
abstract class CheckInRecordDatabase : RoomDatabase() {
    abstract fun checkInDao(): CheckInRecordDao
    abstract fun sideDishDao(): SideDishDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE check_in_records_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        type TEXT NOT NULL DEFAULT '自慰',
                        sideDishes TEXT NOT NULL DEFAULT '',
                        feeling TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                connection.prepare("SELECT date, count FROM check_in_records").use { cursor ->
                    connection.prepare(
                        "INSERT INTO check_in_records_new " +
                            "(date, type, sideDishes, feeling) VALUES (?, '自慰', '', '')"
                    ).use { insertStatement ->
                        while (cursor.step()) {
                            val date = cursor.getText(0)
                            val count = cursor.getInt(1)
                            for (i in 0 until count.coerceAtMost(20)) {
                                insertStatement.bindText(1, date)
                                insertStatement.step()
                                insertStatement.reset()
                                insertStatement.clearBindings()
                            }
                        }
                    }
                }
                connection.execSQL("DROP TABLE check_in_records")
                connection.execSQL("ALTER TABLE check_in_records_new RENAME TO check_in_records")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE check_in_records ADD COLUMN time TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sidedishes (
                        videoCode TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        coverUrl TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val instance: CheckInRecordDatabase by lazy {
            createCheckInRecordDatabaseBuilder()
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration(true)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
    }
}

@Suppress("KotlinNoActualForExpect")
expect object CheckInRecordDatabaseConstructor :
    RoomDatabaseConstructor<CheckInRecordDatabase> {
    override fun initialize(): CheckInRecordDatabase
}
