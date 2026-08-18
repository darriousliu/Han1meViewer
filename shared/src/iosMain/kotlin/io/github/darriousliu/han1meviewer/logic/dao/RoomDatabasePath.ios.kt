package io.github.darriousliu.han1meviewer.logic.dao

import androidx.room3.Room

internal actual val historyDatabase: HistoryDatabase by lazy {
    buildHistoryDatabase(
        Room.databaseBuilder<HistoryDatabase>(
            name = roomDatabasePath("history.db"),
        )
    )
}

internal actual val downloadDatabase: DownloadDatabase by lazy {
    buildDownloadDatabase(
        Room.databaseBuilder<DownloadDatabase>(
            name = roomDatabasePath("download.db"),
        )
    )
}

internal actual val checkInRecordDatabase: CheckInRecordDatabase by lazy {
    buildCheckInRecordDatabase(
        Room.databaseBuilder<CheckInRecordDatabase>(
            name = roomDatabasePath("check_in_records"),
        )
    )
}

internal actual val miscellanyDatabase: MiscellanyDatabase by lazy {
    buildMiscellanyDatabase(
        Room.databaseBuilder<MiscellanyDatabase>(
            name = roomDatabasePath("miscellany.db"),
        )
    )
}
