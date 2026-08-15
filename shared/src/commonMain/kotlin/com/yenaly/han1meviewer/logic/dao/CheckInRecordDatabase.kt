package com.yenaly.han1meviewer.logic.dao

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.yenaly.han1meviewer.logic.entity.CheckInRecordEntity
import com.yenaly.han1meviewer.logic.entity.SideDishEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@ConstructedBy(CheckInRecordDatabaseConstructor::class)
@Database(
    entities = [CheckInRecordEntity::class, SideDishEntity::class],
    version = 4,
    exportSchema = false
)
abstract class CheckInRecordDatabase : RoomDatabase() {
    abstract fun checkInDao(): CheckInRecordDao
    abstract fun sideDishDao(): SideDishDao

    companion object {
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.executeSQL(
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
                val datesWithCounts = buildList {
                    connection.prepare("SELECT date, count FROM check_in_records")
                        .use { statement ->
                            while (statement.step()) {
                                add(statement.getText(0) to statement.getInt(1))
                            }
                        }
                }

                connection.prepare(
                    "INSERT INTO check_in_records_new (date, type, sideDishes, feeling) VALUES (?, '自慰', '', '')"
                ).use { statement ->
                    datesWithCounts.forEach { (date, count) ->
                        repeat(count.coerceAtMost(20)) {
                            statement.bindText(1, date)
                            statement.step()
                            statement.reset()
                            statement.clearBindings()
                        }
                    }
                }
                connection.executeSQL("DROP TABLE check_in_records")
                connection.executeSQL("ALTER TABLE check_in_records_new RENAME TO check_in_records")
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.executeSQL(
                    "ALTER TABLE check_in_records ADD COLUMN time TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.executeSQL(
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

        val instance: CheckInRecordDatabase
            get() = checkInRecordDatabase
    }
}

internal fun buildCheckInRecordDatabase(
    builder: RoomDatabase.Builder<CheckInRecordDatabase>
): CheckInRecordDatabase = builder
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .addMigrations(
        CheckInRecordDatabase.MIGRATION_1_2,
        CheckInRecordDatabase.MIGRATION_2_3,
        CheckInRecordDatabase.MIGRATION_3_4,
    )
    .fallbackToDestructiveMigration(true)
    .build()

@Suppress("KotlinNoActualForExpect")
expect object CheckInRecordDatabaseConstructor : RoomDatabaseConstructor<CheckInRecordDatabase> {
    override fun initialize(): CheckInRecordDatabase
}
