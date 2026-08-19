package io.github.darriousliu.han1meviewer.feature.video

import io.github.darriousliu.han1meviewer.core.common.HanimeResolution
import io.github.darriousliu.han1meviewer.core.model.HanimeVideo
import io.github.darriousliu.han1meviewer.core.ui.preview.fakeHomePageVideos
import io.github.darriousliu.han1meviewer.core.ui.preview.fakeTagList2
import kotlinx.datetime.LocalDate

// @Preview 用的本域假数据，原在 :shared 的 ComposePreviewDataSource.kt。

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
