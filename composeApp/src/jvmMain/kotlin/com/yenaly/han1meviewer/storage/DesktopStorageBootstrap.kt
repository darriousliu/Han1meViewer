package com.yenaly.han1meviewer.storage

import com.ctrip.flight.mmkv.initialize as initializeMmkv
import java.nio.file.Files
import java.nio.file.Path

internal fun initializeDesktopStorage() {
    if (AppStorage.isInstalled) return
    val root = Path.of(System.getProperty("user.home"), ".han1meviewer", "mmkv")
    Files.createDirectories(root)
    initializeMmkv(root.toString())
    AppStorage.prepareAndInstall(
        StorageIssueReporter { issue ->
            System.err.println(
                "Storage ${issue.owner}/${issue.keyName.orEmpty()}/${issue.operation}: " +
                    issue.message,
            )
            issue.cause?.printStackTrace(System.err)
        },
    )
}
