package io.github.darriousliu.han1meviewer.logic.dao

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.div

internal expect val historyDatabase: HistoryDatabase

internal expect val downloadDatabase: DownloadDatabase

internal expect val checkInRecordDatabase: CheckInRecordDatabase

internal expect val miscellanyDatabase: MiscellanyDatabase

internal fun roomDatabasePath(name: String): String =
    (FileKit.databasesDir / name).absolutePath()
