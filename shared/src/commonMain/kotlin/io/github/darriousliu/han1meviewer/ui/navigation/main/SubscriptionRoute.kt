package io.github.darriousliu.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboard
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.darriousliu.han1meviewer.getHanimeSearchShareText
import io.github.darriousliu.han1meviewer.getHanimeShareText
import io.github.darriousliu.han1meviewer.ui.component.LocalToaster
import io.github.darriousliu.han1meviewer.ui.component.showShort
import io.github.darriousliu.han1meviewer.ui.screen.home.SubscriptionScreen
import io.github.darriousliu.han1meviewer.ui.viewmodel.MySubscriptionsViewModel
import io.github.darriousliu.han1meviewer.util.setPlainText
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.copy_to_clipboard
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun SubscriptionRouteScreen(
    onBack: () -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val viewModel: MySubscriptionsViewModel = viewModel()
    val toaster = LocalToaster.current
    val copiedHint = stringResource(Res.string.copy_to_clipboard)
    SubscriptionScreen(
        navigateBack = onBack,
        viewModel = viewModel,
        onClickArtist = { onNavigateToSearch(it) },
        onLongClickArtist = { artistName ->
            scope.launch { clipboard.setPlainText(getHanimeSearchShareText(artistName)) }
            toaster.showShort(copiedHint)
        },
        onClickVideosItem = onNavigateToVideo,
        onLongClickVideosItem = { videoCode, title ->
            scope.launch { clipboard.setPlainText(getHanimeShareText(title, videoCode)) }
            toaster.showShort(copiedHint)
        },
    )
}
