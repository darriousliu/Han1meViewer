package com.yenaly.han1meviewer.logic.dao

import androidx.room.Room
import androidx.room.RoomDatabase
import java.nio.file.Files
import java.nio.file.Path

internal actual fun createHistoryDatabaseBuilder(): RoomDatabase.Builder<HistoryDatabase> =
    Room.databaseBuilder(
        name = databasePath(HISTORY_DATABASE_FILE_NAME),
        factory = HistoryDatabaseConstructor::initialize,
    )

internal actual fun createCheckInRecordDatabaseBuilder(): RoomDatabase.Builder<CheckInRecordDatabase> =
    Room.databaseBuilder(
        name = databasePath(CHECK_IN_DATABASE_FILE_NAME),
        factory = CheckInRecordDatabaseConstructor::initialize,
    )

internal actual fun createDownloadDatabaseBuilder(): RoomDatabase.Builder<DownloadDatabase> =
    Room.databaseBuilder(
        name = databasePath(DOWNLOAD_DATABASE_FILE_NAME),
        factory = DownloadDatabaseConstructor::initialize,
    )

internal actual fun createMiscellanyDatabaseBuilder(): RoomDatabase.Builder<MiscellanyDatabase> =
    Room.databaseBuilder(
        name = databasePath(MISCELLANY_DATABASE_FILE_NAME),
        factory = MiscellanyDatabaseConstructor::initialize,
    )

private val databaseDirectory: Path by lazy {
    Path.of(System.getProperty("user.home"), ".han1meviewer", "databases")
        .also { directory -> Files.createDirectories(directory) }
}

private fun databasePath(fileName: String): String = databaseDirectory.resolve(fileName).toString()
