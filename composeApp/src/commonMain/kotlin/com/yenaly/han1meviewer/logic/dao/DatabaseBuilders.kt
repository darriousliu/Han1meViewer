package com.yenaly.han1meviewer.logic.dao

import androidx.room.RoomDatabase

internal const val HISTORY_DATABASE_FILE_NAME = "history.db"
internal const val CHECK_IN_DATABASE_FILE_NAME = "check_in_records"
internal const val DOWNLOAD_DATABASE_FILE_NAME = "download.db"
internal const val MISCELLANY_DATABASE_FILE_NAME = "miscellany.db"

internal expect fun createHistoryDatabaseBuilder(): RoomDatabase.Builder<HistoryDatabase>

internal expect fun createCheckInRecordDatabaseBuilder(): RoomDatabase.Builder<CheckInRecordDatabase>

internal expect fun createDownloadDatabaseBuilder(): RoomDatabase.Builder<DownloadDatabase>

internal expect fun createMiscellanyDatabaseBuilder(): RoomDatabase.Builder<MiscellanyDatabase>
