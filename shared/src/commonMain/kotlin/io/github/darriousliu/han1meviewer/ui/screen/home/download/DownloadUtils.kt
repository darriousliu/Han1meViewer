package io.github.darriousliu.han1meviewer.ui.screen.home.download

import androidx.compose.runtime.Composable
import io.github.darriousliu.han1meviewer.logic.entity.download.DownloadGroupEntity
import io.github.darriousliu.han1meviewer.logic.entity.download.VideoWithCategories
import io.github.darriousliu.han1meviewer.logic.DownloadHeaderNode
import io.github.darriousliu.han1meviewer.logic.DownloadItemNode
import io.github.darriousliu.han1meviewer.logic.DownloadedNode
import io.github.darriousliu.han1meviewer.core.common.state.DownloadState
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.already_in_queue
import io.github.darriousliu.han1meviewer.core.resource.baseline_error_outline_24
import io.github.darriousliu.han1meviewer.core.resource.download_complete
import io.github.darriousliu.han1meviewer.core.resource.download_failed_tap_retry
import io.github.darriousliu.han1meviewer.core.resource.download_progress_percent
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_check_circle_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_download_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_pause_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_play_arrow_24
import io.github.darriousliu.han1meviewer.core.resource.loading
import io.github.darriousliu.han1meviewer.core.resource.paused
import io.github.darriousliu.han1meviewer.core.resource.ungrouped
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource

/**
 * 将已下载视频列表按分组 ID 转换为 [DownloadHeaderNode] 列表。
 *
 * @param groupIdToNameMap 分组 ID -> 名称映射
 * @param collapseDownloadedGroup 默认是否折叠分组
 * @return 按分组聚合后的 Header 节点列表
 */
fun List<VideoWithCategories>.toNodeList(
    groupIdToNameMap: Map<Int, String>,
    collapseDownloadedGroup: Boolean,
): List<DownloadHeaderNode> {
    // toSortedMap 是 JVM-only 的 stdlib 扩展（返回 java.util.SortedMap），
    // 换成显式按 key 排序，遍历顺序与原来的自然序一致。
    val groupedData = this.groupBy { it.video.groupId }.entries.sortedBy { it.key }
    return buildList {
        for ((groupId, videos) in groupedData) {
            add(
                DownloadHeaderNode(
                    groupKey = groupIdToNameMap[groupId] ?: "ID: $groupId",
                    originalVideos = videos,
                    isExpanded = !collapseDownloadedGroup,
                )
            )
        }
    }
}

/**
 * 将 Header 列表展开为扁平节点列表（Header + 展开状态下的子项）。
 *
 * @return 扁平化的 [DownloadedNode] 列表
 */
fun List<DownloadHeaderNode>.toFlatNodeList(): List<DownloadedNode> {
    val flatList = mutableListOf<DownloadedNode>()
    for (header in this) {
        flatList.add(header)
        if (header.isExpanded) {
            header.originalVideos.forEach { video ->
                flatList.add(DownloadItemNode(video, header.groupKey))
            }
        }
    }
    return flatList
}

/**
 * 将未分组的分组名称替换为"未分组"字符串资源。
 *
 * @param List<DownloadGroupEntity> 分组列表
 * @return 替换后的分组列表
 */
@Composable
fun List<DownloadGroupEntity>.toDisplayGroups(): List<DownloadGroupEntity> = map { group ->
    if (group.id == DownloadGroupEntity.DEFAULT_GROUP_ID) {
        group.copy(name = stringResource(Res.string.ungrouped))
    } else {
        group
    }
}

/**
 * 下载状态对应的显示文本。
 *
 * @param state 下载状态
 * @param progress 下载进度 (0-100)
 * @return 本地化文本
 */
@Composable
fun downloadStateText(state: DownloadState, progress: Int): String = when (state) {
    DownloadState.Queued -> stringResource(Res.string.already_in_queue)
    DownloadState.Downloading -> stringResource(Res.string.download_progress_percent, progress)
    DownloadState.Paused -> stringResource(Res.string.paused)
    DownloadState.Failed -> stringResource(Res.string.download_failed_tap_retry)
    DownloadState.Finished -> stringResource(Res.string.download_complete)
    DownloadState.Unknown -> stringResource(Res.string.loading)
}

/**
 * 下载状态对应的图标资源 ID。
 *
 * @param state 下载状态
 * @return 图标 drawable 资源
 */
fun downloadStateIcon(state: DownloadState): DrawableResource = when (state) {
    DownloadState.Queued -> Res.drawable.ic_baseline_play_arrow_24
    DownloadState.Downloading -> Res.drawable.ic_baseline_pause_24
    DownloadState.Paused -> Res.drawable.ic_baseline_play_arrow_24
    DownloadState.Failed -> Res.drawable.baseline_error_outline_24
    DownloadState.Finished -> Res.drawable.ic_baseline_check_circle_24
    DownloadState.Unknown -> Res.drawable.ic_baseline_download_24
}
