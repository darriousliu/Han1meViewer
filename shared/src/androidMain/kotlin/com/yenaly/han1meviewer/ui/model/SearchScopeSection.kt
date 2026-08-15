package com.yenaly.han1meviewer.ui.model

import com.yenaly.han1meviewer.logic.model.SearchOption
import org.jetbrains.compose.resources.StringResource

data class SearchScopeSection(
    val titleRes: StringResource,
    val options: List<SearchOption>,
    val spanCount: Int = 3,
)