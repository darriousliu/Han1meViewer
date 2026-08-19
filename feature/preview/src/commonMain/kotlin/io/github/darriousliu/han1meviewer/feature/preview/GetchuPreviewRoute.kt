package io.github.darriousliu.han1meviewer.feature.preview

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.darriousliu.han1meviewer.core.navigation.GetchuPreviewDetailRoute
import io.github.darriousliu.han1meviewer.feature.preview.getchu.GetchuPreviewDetailScreen
import io.github.darriousliu.han1meviewer.feature.preview.getchu.GetchuPreviewScreen
import io.github.darriousliu.han1meviewer.feature.preview.getchu.GetchuPreviewViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GetchuPreviewRouteScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
) {
    val viewModel: GetchuPreviewViewModel = koinViewModel()
    GetchuPreviewScreen(
        onBack = onBack,
        onNavigateToDetail = onNavigateToDetail,
        viewModel = viewModel,
    )
}

@Composable
fun GetchuPreviewDetailRouteScreen(
    route: GetchuPreviewDetailRoute,
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToVideoUrl: (String) -> Unit,
) {
    val viewModel: GetchuPreviewViewModel = koinViewModel()
    GetchuPreviewDetailScreen(
        id = route.id,
        onBack = onBack,
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToVideoUrl = onNavigateToVideoUrl,
        viewModel = viewModel,
    )
}
