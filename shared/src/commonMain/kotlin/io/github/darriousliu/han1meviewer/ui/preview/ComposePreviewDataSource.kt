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
