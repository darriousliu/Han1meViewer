package io.github.darriousliu.han1meviewer.core.ui.model

import io.github.darriousliu.han1meviewer.core.model.SearchOption
import org.jetbrains.compose.resources.StringResource

data class SearchScopeSection(
    val titleRes: StringResource,
    val options: List<SearchOption>,
    val spanCount: Int = 3,
)