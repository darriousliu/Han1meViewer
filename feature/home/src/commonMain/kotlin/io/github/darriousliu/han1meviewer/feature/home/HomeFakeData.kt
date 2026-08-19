package io.github.darriousliu.han1meviewer.feature.home

import io.github.darriousliu.han1meviewer.core.model.Announcement
import io.github.darriousliu.han1meviewer.core.model.HomePage
import io.github.darriousliu.han1meviewer.core.ui.preview.fakeHomePageVideos
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.latest_hanime
import io.github.darriousliu.han1meviewer.core.resource.latest_release
import io.github.darriousliu.han1meviewer.core.resource.they_watched

// @Preview 用的本域假数据，原在 :shared 的 ComposePreviewDataSource.kt。

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
