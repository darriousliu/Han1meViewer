@file:Suppress("UNUSED")
package io.github.darriousliu.han1meviewer.ui.preview

import io.github.darriousliu.han1meviewer.core.storage.entity.download.DownloadGroupEntity
import io.github.darriousliu.han1meviewer.core.storage.entity.download.HanimeDownloadEntity
import io.github.darriousliu.han1meviewer.core.storage.entity.download.VideoWithCategories
import io.github.darriousliu.han1meviewer.core.common.HanimeResolution
import io.github.darriousliu.han1meviewer.core.model.Announcement
import io.github.darriousliu.han1meviewer.logic.DownloadHeaderNode
import io.github.darriousliu.han1meviewer.logic.DownloadItemNode
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
import io.github.darriousliu.han1meviewer.ui.screen.home.homepage.HomeCategory
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
val fakePlaylists = listOf(
    Playlists.Playlist(
        listCode = "code1",
        title = "浪漫喜剧精选合集",
        total = 24,
        coverUrl = "https://picsum.photos/300/200?random=1",
    ),
    Playlists.Playlist(
        listCode = "code2",
        title = "动作大片必看榜单",
        total = 18,
        coverUrl = "https://picsum.photos/300/200?random=2",
    ),
    Playlists.Playlist(
        listCode = "code3",
        title = "温暖治愈的日常剧推荐",
        total = 32,
        coverUrl = "https://picsum.photos/300/200?random=3",
    ),
    Playlists.Playlist(
        listCode = "code4",
        title = "悬疑推理高分作品",
        total = 12,
        coverUrl = "https://picsum.photos/300/200?random=4",
    ),
    Playlists.Playlist(
        listCode = "code5",
        title = "经典动画短片集锦",
        total = 45,
        coverUrl = "https://picsum.photos/300/200?random=5",
    ),
)

val fakeBanner = listOf(
    HomePage.Banner(
        title = "【新作】小悪魔の補習計画 - 第1話",
        description = "クラスで一番真面目な委員長が、放課後に秘密の補習を…",
        picUrl = "https://vdownload.hembed.com/image/thumbnail/101573l.jpg",
        videoCode = "101573",
    ),
    HomePage.Banner(
        title = "【新作】小悪魔の補習計画 - 第2話",
        description = "クラスで一番真面目な委員長が、放課後に秘密の補習を…",
        picUrl = "https://vdownload.hembed.com/image/thumbnail/101573l.jpg",
        videoCode = "101573",
    ),
    HomePage.Banner(
        title = "【新作】小悪魔の補習計画 - 第3話",
        description = "クラスで一番真面目な委員長が、放課後に秘密の補習を…",
        picUrl = "https://vdownload.hembed.com/image/thumbnail/101573l.jpg",
        videoCode = "101573",
    )
)

val fakeAnnouncements = listOf(
    Announcement(
        title = "服务器维护通知",
        content = "将于明日凌晨2:00-4:00进行服务器维护，届时可能无法正常访问。",
        priority = 0,
        isActive = true,
    ),
    Announcement(
        title = "新功能上线：AI字幕生成",
        content = "现已支持AI自动生成中文字幕，请在播放器设置中开启体验。",
        priority = 1,
        isActive = true,
    ),
    Announcement(
        title = "社区规范更新",
        content = "为营造更好的社区氛围，我们更新了评论社区规范，请各位用户遵守。",
        priority = 2,
        isActive = true,
    ),
)

val fakeCategories = listOf(
    HomeCategory(
        key = "preview_latest",
        titleRes = Res.string.latest_hanime,
        genre = "裏番",
        videos = fakeHomePageVideos,
    ),
    HomeCategory(
        key = "preview_release",
        titleRes = Res.string.latest_release,
        sort = "最新上市",
        videos = fakeHomePageVideos.shuffled().take(4),
    ),
    HomeCategory(
        key = "preview_watched",
        titleRes = Res.string.they_watched,
        sort = "他們在看",
        videos = fakeHomePageVideos.shuffled().take(5),
    ),
)

val fakeHomePage = HomePage(
    csrfToken = "preview-csrf-token",
    avatarUrl = "https://picsum.photos/128/128?random=avatar",
    username = "Preview User",
    banner = fakeBanner.firstOrNull(),
    latestHanime = fakeHomePageVideos.toMutableList(),
    latestRelease = fakeHomePageVideos.shuffled().toMutableList(),
    ecchiAnime = fakeHomePageVideos.shuffled().toMutableList(),
    shortEpisodeAnime = fakeHomePageVideos.shuffled().toMutableList(),
    twoPointFiveDAnime = fakeHomePageVideos.shuffled().toMutableList(),
    threeDCG = fakeHomePageVideos.shuffled().toMutableList(),
    motionAnime = fakeHomePageVideos.shuffled().toMutableList(),
    twoDAnime = fakeHomePageVideos.shuffled().toMutableList(),
    aiGenerated = fakeHomePageVideos.shuffled().toMutableList(),
    mmd = fakeHomePageVideos.shuffled().toMutableList(),
    cosplay = fakeHomePageVideos.shuffled().toMutableList(),
    watchingNow = fakeHomePageVideos.shuffled().toMutableList(),
    newAnimeTrailer = fakeHomePageVideos.shuffled().toMutableList(),
    userId = "preview-user-id",
)

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

