@file:Suppress("UNUSED")
package io.github.darriousliu.han1meviewer.ui.preview

import io.github.darriousliu.han1meviewer.core.storage.entity.download.DownloadGroupEntity
import io.github.darriousliu.han1meviewer.core.storage.entity.download.HanimeDownloadEntity
import io.github.darriousliu.han1meviewer.core.storage.entity.download.VideoWithCategories
import io.github.darriousliu.han1meviewer.core.common.HanimeResolution
import io.github.darriousliu.han1meviewer.core.model.Announcement
import io.github.darriousliu.han1meviewer.feature.download.DownloadHeaderNode
import io.github.darriousliu.han1meviewer.feature.download.DownloadItemNode
import io.github.darriousliu.han1meviewer.core.model.GetchuPreview
import io.github.darriousliu.han1meviewer.core.model.GetchuPreviewDetail
import io.github.darriousliu.han1meviewer.core.model.HanimeInfo
import io.github.darriousliu.han1meviewer.core.model.HanimePreview
import io.github.darriousliu.han1meviewer.core.model.HanimeVideo
import io.github.darriousliu.han1meviewer.core.model.HomePage
import io.github.darriousliu.han1meviewer.core.model.Playlists
import io.github.darriousliu.han1meviewer.core.model.SubscriptionItem
import io.github.darriousliu.han1meviewer.core.model.SubscriptionVideosItem
import io.github.darriousliu.han1meviewer.core.model.VideoComments
import io.github.darriousliu.han1meviewer.feature.home.HomeCategory
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.latest_hanime
import io.github.darriousliu.han1meviewer.core.resource.latest_release
import io.github.darriousliu.han1meviewer.core.resource.they_watched
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import io.github.darriousliu.han1meviewer.core.ui.preview.fakeTagList2
import io.github.darriousliu.han1meviewer.core.ui.preview.fakeHomePageVideos


/**
 * Compose预览用数据源
 */
val fakeVideoIntroduction = HanimeVideo(
    title = "Shishunki no Obenkyou 2",
    coverUrl = fakeHomePageVideos.first().coverUrl,
    chineseTitle = "思春期的性学习 第2话",
    introduction = "思春期的性学习 2。为了拓展自己的知识，女主开始在图书馆进行一些不太适合公开讨论的研究。\nhttps://hanime1.me/watch?v=101573",
    uploadTime = LocalDate(2024, 5, 10),
    views = "137.6万次",
    videoUrls = io.github.darriousliu.han1meviewer.core.common.HanimeResolution().apply {
        parseResolution("720P", "https://example.com/video.mp4", "video/mp4")
    }.toResolutionLinkMap(),
    tags = fakeTagList2,
    playlist = HanimeVideo.Playlist(
        playlistName = "思春期系列",
        video = fakeHomePageVideos.take(5).mapIndexed { index, item ->
            item.copy(isPlaying = index == 1)
        },
    ),
    relatedHanimes = fakeHomePageVideos,
    artist = HanimeVideo.Artist(
        name = "製作社A",
        avatarUrl = fakeHomePageVideos.first().coverUrl,
        genre = "3D",
        post = HanimeVideo.Artist.POST(
            userId = "1001",
            artistId = "2002",
            isSubscribed = true,
        ),
    ),
    isFav = true,
    currentUserId = "10086",
    originalComic = "https://example.com/comic",
    favTimes = 999
)

