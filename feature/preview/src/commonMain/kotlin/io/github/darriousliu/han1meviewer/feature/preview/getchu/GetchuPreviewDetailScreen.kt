package io.github.darriousliu.han1meviewer.feature.preview.getchu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import coil3.compose.LocalPlatformContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.darriousliu.han1meviewer.core.common.state.PageState
import io.github.darriousliu.han1meviewer.core.common.state.dataOrNull
import io.github.darriousliu.han1meviewer.core.common.util.pienization
import io.github.darriousliu.han1meviewer.core.ui.component.PageContent
import io.github.darriousliu.han1meviewer.core.ui.component.isFirstPageEmpty
import io.github.darriousliu.han1meviewer.core.ui.component.isFirstPageError
import io.github.darriousliu.han1meviewer.core.ui.component.isFirstPageLoading
import io.github.darriousliu.han1meviewer.feature.preview.PreviewImageViewerDialog
import io.github.darriousliu.han1meviewer.feature.preview.PreviewImageViewerState
import io.github.darriousliu.han1meviewer.core.ui.rememberRandomLoadingHint
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.back
import io.github.darriousliu.han1meviewer.core.resource.getchu_preview_detail
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_arrow_back_24
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GetchuPreviewDetailScreen(
    id: String,
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToVideoUrl: (String) -> Unit,
    viewModel: GetchuPreviewViewModel,
) {
    val detailState = remember(id) { viewModel.detailState(id) }
    val state = detailState.collectAsStateWithLifecycle().value
    var imageViewerState by remember { mutableStateOf<PreviewImageViewerState?>(null) }
    val loadingHint = rememberRandomLoadingHint()
    val context = LocalPlatformContext.current
    val imageLoader = remember {
        createGetchuImageLoader(context)
    }
    LaunchedEffect(id) { viewModel.getDetail(id) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(stringResource(Res.string.getchu_preview_detail)) },
            navigationIcon = {
                FilledIconButton(onClick = onBack) {
                    Icon(
                        painterResource(Res.drawable.ic_baseline_arrow_back_24),
                        stringResource(Res.string.back)
                    )
                }
            },
        )
        PageContent(
            isLoading = state.isFirstPageLoading,
            isError = state.isFirstPageError,
            isEmpty = state.isFirstPageEmpty,
            errorMessage = (state as? PageState.Error)?.throwable?.pienization().orEmpty(),
            onRetry = { viewModel.getDetail(id) },
            modifier = Modifier.fillMaxSize(),
            loadingMessage = loadingHint
        ) {
            state.dataOrNull?.let { detail ->
                GetchuPreviewDetailContent(
                    detail = detail,
                    onOpenImage = { index, images ->
                        imageViewerState = PreviewImageViewerState(images, index)
                    },
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToVideoUrl = onNavigateToVideoUrl,
                    imageLoader = imageLoader
                )
            }
        }
    }

    imageViewerState?.let { viewerState ->
        PreviewImageViewerDialog(
            imageUrls = viewerState.imageUrls,
            initialPage = viewerState.initialPage,
            onDismiss = { imageViewerState = null },
            imageLoader = imageLoader,
        )
    }
}
