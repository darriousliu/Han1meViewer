package io.github.darriousliu.han1meviewer.feature.home

import io.github.darriousliu.han1meviewer.core.model.Announcement
import org.jetbrains.compose.resources.StringResource

/**
 * 主页 UI 事件集合，用于在主界面（Home）中处理用户交互行为，功能如函数名所写。
 *
 */

sealed interface HomeUiEvent {
    data object OpenDrawer : HomeUiEvent
    data object NavigateToPreview : HomeUiEvent
    data class OpenSearchPage(val query: String = "") : HomeUiEvent
    data class NavigateToSearchAdvanced(val params: Map<String, String>) : HomeUiEvent
    data class OpenVideo(val videoCode: String) : HomeUiEvent
    data class LongPressVideoCopy(val videoCode: String, val videoTitle: String) : HomeUiEvent
    data object ShowExitDialog : HomeUiEvent
    data class ShowAnnouncementDialog(val announcement: Announcement) : HomeUiEvent

    /** 下拉刷新失败但还有缓存内容时提示一下，由 route 决定怎么显示（Android 是 Toast） */
    data class ShowRefreshError(val messageRes: StringResource) : HomeUiEvent
}