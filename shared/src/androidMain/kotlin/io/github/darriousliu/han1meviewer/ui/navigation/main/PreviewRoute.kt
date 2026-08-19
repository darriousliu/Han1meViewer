package io.github.darriousliu.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.darriousliu.han1meviewer.feature.preview.PreviewScreen
import io.github.darriousliu.han1meviewer.feature.comment.CommentViewModel
import io.github.darriousliu.han1meviewer.feature.preview.PreviewViewModel
import io.github.darriousliu.han1meviewer.feature.comment.PreviewCommentPrefetcher
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PreviewRouteScreen(
    onBack: () -> Unit,
    onNavigateToGetchuPreview: () -> Unit,
    onNavigateToPreviewComment: (String, String) -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val previewViewModel: PreviewViewModel = koinViewModel()
    // 预览页这个实例只是 PreviewCommentPrefetcher 的预取缓冲，拉哪个月由 fetch() 显式传，
    // 自身 code 不参与逻辑，所以用 prefetchFactory()。挂本 NavEntry：评论页始终叠在
    // 预览页之上，预览 entry 在栈内 store 就活着，预取数据足以覆盖两页共享期。
    val commentViewModel: CommentViewModel = viewModel(
        key = CommentViewModel.PREFETCH_KEY,
        factory = CommentViewModel.prefetchFactory(),
    )

    PreviewScreen(
        onBack = onBack,
        onNavigateToGetchuPreview = onNavigateToGetchuPreview,
        onNavigateToPreviewComment = onNavigateToPreviewComment,
        onNavigateToVideo = onNavigateToVideo,
        previewViewModel = previewViewModel,
        commentViewModel = commentViewModel,
    )
}
