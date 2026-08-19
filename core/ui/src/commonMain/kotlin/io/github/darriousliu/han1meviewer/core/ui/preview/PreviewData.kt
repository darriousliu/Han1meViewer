package io.github.darriousliu.han1meviewer.core.ui.preview

import io.github.darriousliu.han1meviewer.core.model.HanimeInfo
import io.github.darriousliu.han1meviewer.core.model.SubscriptionItem
import io.github.darriousliu.han1meviewer.core.model.SubscriptionVideosItem

/**
 * `@Preview` 用的假数据里，只依赖 :core:model 的那部分。
 *
 * 其余（首页分类、下载分组等）依赖 :shared 的业务类型，留在
 * `ui/preview/ComposePreviewDataSource.kt`——core:ui 不认识那些。
 */

val fakeArtists = listOf(
    SubscriptionItem("初音未来", "null"),
    SubscriptionItem("绫波丽", "null"),
    SubscriptionItem("阿库娅", "null"),
    SubscriptionItem("初音未来", "null"),
    SubscriptionItem("绫波丽", "null"),
    SubscriptionItem("阿库娅", "null"),
    SubscriptionItem("初音未来", "null"),
    SubscriptionItem("绫波丽", "null"),
    SubscriptionItem("阿库娅", "null"),
    SubscriptionItem("阿库娅", "null"),
    SubscriptionItem("初音未来", "null"),
    SubscriptionItem("绫波丽", "null"),
    SubscriptionItem("阿库娅", "null"),
    SubscriptionItem("阿库娅", "null"),
    SubscriptionItem("初音未来", "null"),
    SubscriptionItem("绫波丽", "null"),
    SubscriptionItem("阿库娅", "null"),
    SubscriptionItem("阿库娅", "null"),
    SubscriptionItem("初音未来", "null"),
    SubscriptionItem("绫波丽", "null"),
    SubscriptionItem("阿库娅", "null"),
)

val fakeVideosItem = SubscriptionVideosItem(
    title = "小恶魔的补习计划",
    coverUrl = "https://vdownload.hembed.com/image/thumbnail/101573l.jpg",
    videoCode = "101573",
    duration = "04:34",
    views = "44.9万次",
    reviews = "100%",
    uploadTime = "2020-12-12",
)

val fakeTagList2 = listOf(
    "新番",
    "预告",
    "校园",
    "妹妹",
    "姐系",
    "正太",
    "萝莉",
    "伪娘",
    "NTR",
    "SM",
    "暴力",
    "GURO",
    "血腥",
    "人妻"
)

const val longText =
    "这是一段用于预览的简介文本，包含一个链接 https://hanime1.me/watch?v=101573 ，用于验证展开和收" +
            "起功能是否正常。为了触发折叠，这里再补充一些额外内容。超长文本超长文本超长文本超长文本超长文本超长文本超长文本" +
            "超长文本超长文本超长文本超长文本超长文本超长文本超长文本超长文本超长文本超长文本超长文本超长文本超长文本"

val fakeHomePageVideos = listOf(
    HanimeInfo(
        title = "小恶魔的补习计划",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101573l.jpg",
        videoCode = "101573",
        duration = "04:34",
        views = "44.9万次",
        reviews = "100%",
        currentArtist = "製作社A",
        uploadTime = "2010-12-10",
        itemType = HanimeInfo.NORMAL,
    ),
    HanimeInfo(
        title = "姐姐的秘密训练",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101574l.jpg",
        videoCode = "101574",
        duration = "23:15",
        views = "22.1万次",
        reviews = "95%",
        currentArtist = "製作社B",
        uploadTime = "2010-12-10",
        itemType = HanimeInfo.NORMAL,
    ),
    HanimeInfo(
        title = "放学后的约定",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101575l.jpg",
        videoCode = "101575",
        duration = "18:02",
        views = "58.3万次",
        reviews = "97%",
        currentArtist = "製作社C",
        uploadTime = "2010-12-10",
        itemType = HanimeInfo.NORMAL,
    ),
    HanimeInfo(
        title = "班长的福利日",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101576l.jpg",
        videoCode = "101576",
        duration = "12:47",
        views = "30.0万次",
        reviews = "92%",
        currentArtist = "製作社D",
        uploadTime = "2010-12-10",
        itemType = HanimeInfo.NORMAL,
    ),
    HanimeInfo(
        title = "图书馆的秘密角落",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101577l.jpg",
        videoCode = "101577",
        duration = "15:20",
        views = "61.7万次",
        reviews = "99%",
        currentArtist = "製作社E",
        uploadTime = "2010-12-10",
        itemType = HanimeInfo.NORMAL,
    ),
    HanimeInfo(
        title = "体育仓库的约定",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101588l.jpg",
        videoCode = "101588",
        duration = "22:10",
        views = "35.2万次",
        reviews = "94%",
        currentArtist = "製作社F",
        uploadTime = "2011-01-15",
        itemType = HanimeInfo.NORMAL,
    ),
)

val fakeVideos = listOf(
    SubscriptionVideosItem(
        title = "小恶魔的补习计划",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101573l.jpg",
        videoCode = "101573",
        duration = "04:34",
        views = "44.9万次",
        reviews = "100%",
        uploadTime = "2010-12-10",
    ),
    SubscriptionVideosItem(
        title = "姐姐的秘密训练",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101574l.jpg",
        videoCode = "101574",
        duration = "23:15",
        views = "22.1万次",
        reviews = "95%",
        uploadTime = "2010-12-10",
    ),
    SubscriptionVideosItem(
        title = "放学后的约定",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101575l.jpg",
        videoCode = "101575",
        duration = "18:02",
        views = "58.3万次",
        reviews = "97%",
        uploadTime = "2010-12-10",
    ),
    SubscriptionVideosItem(
        title = "班长的福利日",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101576l.jpg",
        videoCode = "101576",
        duration = "12:47",
        views = "30.0万次",
        reviews = "92%",
        uploadTime = "2010-12-10",
    ),
    SubscriptionVideosItem(
        title = "图书馆的秘密角落",
        coverUrl = "https://vdownload.hembed.com/image/thumbnail/101577l.jpg",
        videoCode = "101577",
        duration = "15:20",
        views = "61.7万次",
        reviews = "99%",
        uploadTime = "2010-12-10",
    ),
)
