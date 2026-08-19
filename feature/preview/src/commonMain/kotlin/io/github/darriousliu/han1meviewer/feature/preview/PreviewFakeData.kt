package io.github.darriousliu.han1meviewer.feature.preview

import io.github.darriousliu.han1meviewer.core.model.GetchuPreview
import io.github.darriousliu.han1meviewer.core.model.GetchuPreviewDetail
import io.github.darriousliu.han1meviewer.core.model.HanimePreview
import kotlinx.datetime.LocalDate
import io.github.darriousliu.han1meviewer.core.ui.preview.fakeHomePageVideos
import io.github.darriousliu.han1meviewer.core.ui.preview.fakeTagList1
import io.github.darriousliu.han1meviewer.core.ui.preview.fakeTagList2

// @Preview 用的本域假数据，原在 :shared 的 ComposePreviewDataSource.kt。

val fakeNewHanimeInfo = listOf(
    HanimePreview.PreviewInfo(
        title = "日文标题",
        videoTitle = "中文标题 第1话",
        coverUrl = fakeHomePageVideos.first().coverUrl,
        introduction = "这是用于预览的简介内容，用来确认 Compose 版布局是否正常。",
        brand = "发行商 A",
        releaseDate = "2024-01-01",
        videoCode = fakeHomePageVideos.first().videoCode,
        tags = fakeTagList1,
        relatedPicsUrl = listOf(
            fakeHomePageVideos[1].coverUrl,
            fakeHomePageVideos[2].coverUrl
        ),
    ),
    HanimePreview.PreviewInfo(
        title = "日文标题2",
        videoTitle = "中文标题 第2话",
        coverUrl = fakeHomePageVideos.first().coverUrl,
        introduction = "这是用于预览的简介内容，用来确认 Compose 版布局是否正常。",
        brand = "发行商 B",
        releaseDate = "2022-05-02",
        videoCode = fakeHomePageVideos.first().videoCode,
        tags = fakeTagList2,
        relatedPicsUrl = listOf(
            fakeHomePageVideos[1].coverUrl,
            fakeHomePageVideos[2].coverUrl
        ),
    )
)

val fakeGetchuPreviewDetail = GetchuPreviewDetail(
    id = "1364146",
    title = "1LDK＋J系 いきなり同居？密着!?初エッチ!!? 第8話",
    brand = "King Bee",
    coverUrl = "https://www.getchu.com/brandnew/1364146/rc1364146package.jpg",
    description = "「二三月そう」原作「1LDK＋JK いきなり同居？密着!?初エッチ！!?」（出版：KATTS）OVA化第8弾！\n" +
            "休日の二人はいつもより大胆に",
    releaseDate = "2026/07/10",
    price = "￥4,200",
    productUrl = "https://www.getchu.com/item/1364146/?gc=gc",
    videoUrls = listOf("https://www.moongirls.us/getchu/2026/07/1364146.mp4"),
    sections = listOf(
        GetchuPreviewDetail.TextSection(
            title = "123",
            body = "456"
        ),
        GetchuPreviewDetail.TextSection(
            title = "456",
            body = "789"
        )
    ),
    sampleImages = listOf(
        "https://www.getchu.com/brandnew/1364146/c1364146sample1_s.jpg",
        "https://www.getchu.com/brandnew/1364146/c1364146sample1_s.jpg"
    ),
    seriesItems = listOf(
        GetchuPreview.Item(
            id = "1364146",
            title = "1LDK＋J系 いきなり同居？密着!?初エッチ!!? 第8話",
            brand = "King Bee",
            coverUrl = "https://www.getchu.com/brandnew/1364146/rc1364146package.jpg",
            detailUrl = "https://www.getchu.com/item/1364146/?gc=gc",
            price = "￥4,200"
        )
    ),
    relatedItems = listOf(
        GetchuPreview.Item(
            id = "1364146",
            title = "1LDK＋J系 いきなり同居？密着!?初エッチ!!? 第9話",
            brand = "King Bee",
            coverUrl = "https://www.getchu.com/brandnew/1364146/rc1364146package.jpg",
            detailUrl = "https://www.getchu.com/item/1364146/?gc=gc",
            price = "￥4,200"
        )
    )
)

val fakeGetchuPreviewItem = GetchuPreview.Item(
    id = "1364146",
    title = "1LDK＋J系 いきなり同居？密着!?初エッチ!!? 第8話",
    brand = "King Bee",
    coverUrl = "https://www.getchu.com/brandnew/1364146/rc1364146package.jpg",
    detailUrl = "https://www.getchu.com/item/1364146/?gc=gc",
    price = "￥4,200"
)

// 用于Compose预览的静态假数据

val fakeGetchuPreview = GetchuPreview(
    dateCode = "2024-01",
    groups = listOf(
        GetchuPreview.Group(
            releaseDate = "2024年1月15日",
            items = listOf(
                GetchuPreview.Item(
                    id = "item_001",
                    title = "【限定版】美少女戦士セーラームーン フィギュア",
                    brand = "GOOD SMILE COMPANY",
                    coverUrl = "https://example.com/image/001.jpg",
                    detailUrl = "https://example.com/detail/001",
                    price = "¥12,800"
                ),
                GetchuPreview.Item(
                    id = "item_002",
                    title = "【予約】鬼滅の刃 竈門炭治郎 1/8スケール",
                    brand = "BANDAI SPIRITS",
                    coverUrl = "https://example.com/image/002.jpg",
                    detailUrl = "https://example.com/detail/002",
                    price = "¥9,800"
                ),
                GetchuPreview.Item(
                    id = "item_003",
                    title = "【再販】進撃の巨人 リヴァイ アクションフィギュア",
                    brand = null,  // brand可为空
                    coverUrl = null, // coverUrl可为空
                    detailUrl = "https://example.com/detail/003",
                    price = null // price可为空
                )
            )
        ),
        GetchuPreview.Group(
            releaseDate = "2024年2月15日",
            items = listOf(
                GetchuPreview.Item(
                    id = "item_004",
                    title = "Fate/Grand Order セイバー アルター",
                    brand = "KOTOBUKIYA",
                    coverUrl = "https://example.com/image/004.jpg",
                    detailUrl = "https://example.com/detail/004",
                    price = "¥15,400"
                ),
                GetchuPreview.Item(
                    id = "item_005",
                    title = "呪術廻戦 五条悟 1/7スケールフィギュア",
                    brand = "MAX FACTORY",
                    coverUrl = "https://example.com/image/005.jpg",
                    detailUrl = "https://example.com/detail/005",
                    price = "¥18,700"
                )
            )
        )
    )
)
