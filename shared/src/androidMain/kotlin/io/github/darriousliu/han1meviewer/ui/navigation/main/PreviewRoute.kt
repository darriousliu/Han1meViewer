package io.github.darriousliu.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.darriousliu.han1meviewer.ui.activity.MainActivity
import io.github.darriousliu.han1meviewer.ui.screen.home.PreviewScreen
import io.github.darriousliu.han1meviewer.ui.viewmodel.CommentViewModel
import io.github.darriousliu.han1meviewer.ui.viewmodel.PreviewViewModel

@Composable
fun PreviewRouteScreen(
    activity: MainActivity,
    onBack: () -> Unit,
    onNavigateToGetchuPreview: () -> Unit,
    onNavigateToPreviewComment: (String, String) -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val previewViewModel: PreviewViewModel = viewModel()
    // 预览页这个实例只是 PreviewCommentPrefetcher 的预取缓冲，拉哪个月由 fetch() 显式传，
    // 自身 code 不参与逻辑，所以用 prefetchFactory()。
    val commentViewModel: CommentViewModel = viewModel(
        viewModelStoreOwner = activity,
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
