package com.yenaly.han1meviewer.logic.dao

import androidx.room.Room
import androidx.room.RoomDatabase
import com.yenaly.yenaly_libs.utils.applicationContext

internal actual fun createHistoryDatabaseBuilder(): RoomDatabase.Builder<HistoryDatabase> =
    Room.databaseBuilder(
        context = applicationContext,
        name = applicationContext.getDatabasePath(HISTORY_DATABASE_FILE_NAME).absolutePath,
        factory = HistoryDatabaseConstructor::initialize,
    )

internal actual fun createCheckInRecordDatabaseBuilder(): RoomDatabase.Builder<CheckInRecordDatabase> =
    Room.databaseBuilder(
        context = applicationContext,
        name = applicationContext.getDatabasePath(CHECK_IN_DATABASE_FILE_NAME).absolutePath,
        factory = CheckInRecordDatabaseConstructor::initialize,
    )

internal actual fun createDownloadDatabaseBuilder(): RoomDatabase.Builder<DownloadDatabase> =
    Room.databaseBuilder(
        context = applicationContext,
        name = applicationContext.getDatabasePath(DOWNLOAD_DATABASE_FILE_NAME).absolutePath,
        factory = DownloadDatabaseConstructor::initialize,
    )

internal actual fun createMiscellanyDatabaseBuilder(): RoomDatabase.Builder<MiscellanyDatabase> =
    Room.databaseBuilder(
        context = applicationContext,
        name = applicationContext.getDatabasePath(MISCELLANY_DATABASE_FILE_NAME).absolutePath,
        factory = MiscellanyDatabaseConstructor::initialize,
    )
