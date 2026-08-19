package io.github.darriousliu.han1meviewer.feature.download

import io.github.darriousliu.han1meviewer.core.storage.entity.download.DownloadGroupEntity
import io.github.darriousliu.han1meviewer.core.storage.entity.download.HanimeDownloadEntity
import io.github.darriousliu.han1meviewer.core.storage.entity.download.VideoWithCategories
import kotlin.time.Clock
import io.github.darriousliu.han1meviewer.core.ui.preview.fakeHomePageVideos

// @Preview 用的本域假数据，原在 :shared 的 ComposePreviewDataSource.kt。

val fakeDownloadedVideos = fakeHomePageVideos.take(3).mapIndexed { index, item ->
    VideoWithCategories(
        video = HanimeDownloadEntity(
            groupId = 1,
            coverUrl = item.coverUrl,
            coverUri = null,
            title = item.title,
            addDate = Clock.System.now().toEpochMilliseconds(),
            videoCode = item.videoCode,
            videoUri = "test$index.mp4",
            quality = "720P",
            videoUrl = "https://example.com/test$index.mp4",
            length = 100L * 1024 * 1024,
            downloadedLength = 100L * 1024 * 1024,
            state = io.github.darriousliu.han1meviewer.core.common.state.DownloadState.Finished,
            id = index + 1,
        ),
        categories = emptyList(),
    )
}

val fakeDownloadedGroups = listOf(DownloadGroupEntity(name = "未分组", orderIndex = 0, id = 1))

val fakeDownloadedNodes = listOf(
    DownloadHeaderNode(
        groupKey = "未分组",
        originalVideos = fakeDownloadedVideos,
        isExpanded = true
    ),
    DownloadItemNode(fakeDownloadedVideos[0], "未分组"),
    DownloadItemNode(fakeDownloadedVideos[1], "未分组"),
    DownloadHeaderNode(
        groupKey = "分组1",
        originalVideos = fakeDownloadedVideos,
        isExpanded = true
    ),
    DownloadItemNode(fakeDownloadedVideos[0], "分组1"),
)
