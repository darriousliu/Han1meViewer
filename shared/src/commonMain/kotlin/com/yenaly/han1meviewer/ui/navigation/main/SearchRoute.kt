package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboard
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.getHanimeShareText
import com.yenaly.han1meviewer.ui.component.LocalToaster
import com.yenaly.han1meviewer.ui.component.showShort
import com.yenaly.han1meviewer.ui.navigation.SearchRoute
import com.yenaly.han1meviewer.ui.screen.search.AdvancedSearchSheet
import com.yenaly.han1meviewer.ui.screen.search.SearchScreen
import com.yenaly.han1meviewer.ui.viewmodel.SearchViewModel
import com.yenaly.han1meviewer.util.setPlainText
import han1meviewer.shared.generated.resources.Res
import han1meviewer.shared.generated.resources.copy_to_clipboard
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource

@Composable
fun SearchRouteScreen(
    route: SearchRoute,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val viewModel: SearchViewModel = viewModel()
    val toaster = LocalToaster.current
    val copiedHint = stringResource(Res.string.copy_to_clipboard)
    var showAdvancedSearchSheet by remember { mutableStateOf(false) }

    LaunchedEffect(route.advancedSearchJson) {
        route.advancedSearchJson?.let { json ->
            runCatching { Json.decodeFromString<Map<String, String>>(json) }
                .onSuccess { params ->
                    params.forEach { (key, value) ->
                        when (key.uppercase()) {
                            "QUERY" -> viewModel.query = value
                            "GENRE" -> viewModel.genre = value
                            "SORT" -> viewModel.sort = value
                            "YEAR" -> viewModel.year = value.toIntOrNull()
                            "MONTH" -> viewModel.month = value.toIntOrNull()
                            "DURATION" -> viewModel.duration = value
                        }
                    }
                }
        }
    }

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
