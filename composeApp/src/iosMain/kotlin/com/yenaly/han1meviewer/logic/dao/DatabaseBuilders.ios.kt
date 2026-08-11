@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.yenaly.han1meviewer.logic.dao

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

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

private val databaseDirectory: String by lazy {
    val applicationSupport = NSSearchPathForDirectoriesInDomains(
        NSApplicationSupportDirectory,
        NSUserDomainMask,
        true,
    ).firstOrNull() as? String ?: error("Application Support directory is unavailable")
    val directory = "$applicationSupport/databases"
    val fileManager = NSFileManager.defaultManager
    check(
        fileManager.createDirectoryAtPath(directory, true, null, null) ||
            fileManager.fileExistsAtPath(directory)
    ) { "Unable to create database directory: $directory" }
    directory
}

private fun databasePath(fileName: String): String = "$databaseDirectory/$fileName"
