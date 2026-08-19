package io.github.darriousliu.han1meviewer.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboard
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.darriousliu.han1meviewer.core.storage.getHanimeShareText
import io.github.darriousliu.han1meviewer.core.ui.component.LocalToaster
import io.github.darriousliu.han1meviewer.core.ui.component.showShort
import io.github.darriousliu.han1meviewer.core.navigation.SearchRoute
import io.github.darriousliu.han1meviewer.core.common.util.setPlainText
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.copy_to_clipboard
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun SearchRouteScreen(
    route: SearchRoute,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    // 路由初始参数（query / 高级筛选 JSON）构造期注入，不再经 effect 写 VM——
    // 那种写法和屏幕内「有筛选就自动搜」的 effect 之间存在执行顺序竞态
    val viewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.factory(
            initialQuery = route.query,
            advancedSearchJson = route.advancedSearchJson,
        ),
    )
    val toaster = LocalToaster.current
    val copiedHint = stringResource(Res.string.copy_to_clipboard)
    var showAdvancedSearchSheet by remember { mutableStateOf(false) }

    if (showAdvancedSearchSheet) {
        AdvancedSearchSheet(
            viewModel = viewModel,
            onDismiss = { showAdvancedSearchSheet = false },
        )
    }

    SearchScreen(
        viewModel = viewModel,
        initialQuery = route.query,
        onBack = onBack,
        onOpenVideo = onNavigateToVideo,
        onLongPressCopy = { videoCode, title ->
            scope.launch { clipboard.setPlainText(getHanimeShareText(title, videoCode)) }
            toaster.showShort(copiedHint)
        },
        onOpenAdvancedSearch = { showAdvancedSearchSheet = true },
    )
}
