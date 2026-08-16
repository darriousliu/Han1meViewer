package com.yenaly.han1meviewer.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.yenaly.han1meviewer.HanimeConstants.HANIME_URL
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.DatabaseRepo.HanimeAdvancedSearchRepo.toSearchOptionSet
import com.yenaly.han1meviewer.logic.NetworkRepo
import com.yenaly.han1meviewer.logic.entity.HanimeAdvancedSearchHistoryEntity
import com.yenaly.han1meviewer.logic.entity.SearchHistoryEntity
import com.yenaly.han1meviewer.logic.model.HanimeInfo
import com.yenaly.han1meviewer.logic.model.SearchOption
import com.yenaly.han1meviewer.logic.state.PageLoadingState
import com.yenaly.han1meviewer.util.loadBundledJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val logger = Logger.withTag("SearchViewModel")

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/13 013 22:29
 */
class SearchViewModel(
    private val state: SavedStateHandle
) : ViewModel() {

    data class SearchOptionsState(
        val genres: List<SearchOption> = emptyList(),
        val tags: Map<String, List<SearchOption>> = emptyMap(),
        val brands: List<SearchOption> = emptyList(),
        val sortOptions: List<SearchOption> = emptyList(),
        val durations: List<SearchOption> = emptyList(),
        val timeList: List<SearchOption> = emptyList(),
    )

    var page: Int = 1
    var query: String?
        get() = state["query"]
        set(value) { state["query"] = value }

    var genre: String?
        get() = state["genre"]
        set(value) { state["genre"] = value }

    var sort: String?
        get() = state["sort"]
        set(value) { state["sort"] = value }

    var year: Int?
        get() = state["year"]
        set(value) { state["year"] = value }

    var month: Int?
        get() = state["month"]
        set(value) { state["month"] = value }

    var approxTime: String?
        get() = state["approxTime"]
        set(value) { state["approxTime"] = value }

    var broad: Boolean
        get() = state["broad"] ?: false
        set(value) { state["broad"] = value }

    var duration: String?
        get() = state["duration"]
        set(value) { state["duration"] = value }

    var gridFirstVisibleItemIndex: Int
        get() = state["gridFirstVisibleItemIndex"] ?: 0
        set(value) {
            state["gridFirstVisibleItemIndex"] = value
        }

    var gridFirstVisibleItemScrollOffset: Int
        get() = state["gridFirstVisibleItemScrollOffset"] ?: 0
        set(value) {
            state["gridFirstVisibleItemScrollOffset"] = value
        }

    var tagMap = mutableMapOf<String, Set<SearchOption>>()
    var brandMap = mutableMapOf<String, Set<SearchOption>>()

    private val _searchOptions = MutableStateFlow(SearchOptionsState())
    val searchOptions = _searchOptions.asStateFlow()

    val genres get() = _searchOptions.value.genres
    val tags get() = _searchOptions.value.tags
    val brands get() = _searchOptions.value.brands
    val sortOptions get() = _searchOptions.value.sortOptions
    val durations get() = _searchOptions.value.durations
    val timeList get() = _searchOptions.value.timeList

    init {
        viewModelScope.launch {
            val genreFile = if (Preferences.baseUrl == HANIME_URL[3]) {
                "genre_av.json"
            } else {
                "genre.json"
            }
            _searchOptions.value = SearchOptionsState(
                genres = loadBundledJson<List<SearchOption>>(
                    "files/search_options/$genreFile"
                ).orEmpty(),
                tags = loadBundledJson<Map<String, List<SearchOption>>>(
                    "files/search_options/tags.json"
                ).orEmpty(),
                brands = loadBundledJson<List<SearchOption>>(
                    "files/search_options/brands.json"
                ).orEmpty(),
                sortOptions = loadBundledJson<List<SearchOption>>(
                    "files/search_options/sort_option.json"
                ).orEmpty(),
                durations = loadBundledJson<List<SearchOption>>(
                    "files/search_options/duration.json"
                ).orEmpty(),
                timeList = loadBundledJson<List<SearchOption>>(
                    "files/search_options/release_date.json"
                ).orEmpty(),
            )
        }
    }

    private val _searchStateFlow =
        MutableStateFlow<PageLoadingState<List<HanimeInfo>>>(PageLoadingState.Loading)
    val searchStateFlow = _searchStateFlow.asStateFlow()

    private val _searchFlow = MutableStateFlow(emptyList<HanimeInfo>())
    val searchFlow = _searchFlow.asStateFlow()

    fun clearHanimeSearchResult() {
        _searchFlow.value = emptyList()
        _searchStateFlow.value = PageLoadingState.Loading
    }

    fun resetSearchUiState() {
        page = 1
        query = null
        genre = null
        sort = null
        year = null
        month = null
        approxTime = null
        broad = false
        duration = null
        tagMap.clear()
        brandMap.clear()
        gridFirstVisibleItemIndex = 0
        gridFirstVisibleItemScrollOffset = 0
        _searchFlow.value = emptyList()
        _searchStateFlow.value = PageLoadingState.Loading
    }

    fun getHanimeSearchResult(
        page: Int, query: String?, genre: String?,
        sort: String?, broad: Boolean, date: String?,
        duration: String?, tags: Set<String>, brands: Set<String>,
    ) {
        viewModelScope.launch {
            NetworkRepo.getHanimeSearchResult(
                page, query, genre,
                sort, broad, date ,
                duration, tags, brands
            ).collect { state ->
                val prev = _searchStateFlow.getAndUpdate { state }
                if (prev is PageLoadingState.Loading) _searchFlow.value = emptyList()
                _searchFlow.update { prevList ->
                    when (state) {
//                        is PageLoadingState.Success -> prevList + state.info
                        is PageLoadingState.Success -> {
                            val list = state.info
                            val updatedList = if (Preferences.showPlayedIndicator) {
                                val codes = list.map { it.videoCode }
                                val watchedCodes = withContext(Dispatchers.IO) {
                                    DatabaseRepo.WatchHistory.getWatched(codes).toSet()
                                }
                                list.map { item ->
                                    item.copy(watched = watchedCodes.contains(item.videoCode))
                                }
                            } else {
                                list
                            }
                            (prevList + updatedList).distinctBy(HanimeInfo::videoCode)
                        }
                        is PageLoadingState.Loading -> emptyList()
                        else -> prevList
                    }
                }
            }
        }
    }

    fun insertSearchHistory(history: SearchHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.SearchHistory.insert(history)
            logger.d { "insert search history: $history" }
        }
    }

    fun getSearchDate(): String? {
        return when {
            approxTime != null -> approxTime
            year != null -> listOfNotNull(
                year?.let { "$it 年" },
                month?.let { "$it 月" }
            ).joinToString(" ")
            else -> null
        }
    }

    fun insertAdvancedSearchHistory(
        query: String?, genre: String?,
        sort: String?, broad: Boolean, date: String?,
        duration: String?, tags: Set<SearchOption>, brands: Set<SearchOption>,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val histories = DatabaseRepo.HanimeAdvancedSearchRepo.getSearchHistories(limit = 10)
                .first()

            val isDuplicate = histories.any { history ->
                history.query == query &&
                        history.genre == genre &&
                        history.sort == sort &&
                        history.broad == broad &&
                        history.date == date &&
                        history.duration == duration &&
                        history.tags?.toSearchOptionSet() == tags &&
                        history.brands?.toSearchOptionSet() == brands
            }

            if (!isDuplicate) {
                DatabaseRepo.HanimeAdvancedSearchRepo.saveSearch(
                    query = query,
                    genre = genre,
                    sort = sort,
                    broad = broad,
                    date = date,
                    duration = duration,
                    tags = tags,
                    brands = brands,
                )
                return@launch
            }
            logger.i { "advanced search history already exists" }

        }
    }

    fun deleteSearchHistory(history: SearchHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.SearchHistory.delete(history)
            logger.d { "delete search history: $history" }
        }
    }

    fun loadAllSearchHistories(keyword: String? = null) =
        DatabaseRepo.SearchHistory.loadAll(keyword).flowOn(Dispatchers.IO)

    fun deleteSearchHistoryByKeyword(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.SearchHistory.deleteByKeyword(query)
            logger.d { "delete search history by keyword: $query" }
        }
    }
    val refreshTriggerFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    fun triggerNewSearch() {
        page = 1
        clearHanimeSearchResult()
        refreshTriggerFlow.tryEmit(Unit)
    }
    fun restoreSearchMap(history: HanimeAdvancedSearchHistoryEntity) {
        with(this) {
            page = 1
            query = history.query
            genre = history.genre
            sort = history.sort
            broad = history.broad == true
            duration = history.duration

            restoreDate(this, history.date)

            tagMap.clear()
            brandMap.clear()

            history.tags?.takeIf { it.isNotBlank() }?.let { tagsString ->
                val tagOptions = tagsString.toSearchOptionSet()
                tagMap.put("", tagOptions)
            }

            history.brands?.takeIf { it.isNotBlank() }?.let { brandsString ->
                val brandOptions = brandsString.toSearchOptionSet()
                brandMap.put("", brandOptions)
            }
        }
    }
    private fun restoreDate(viewModel: SearchViewModel, date: String?) {
        if (date.isNullOrBlank()) {
            viewModel.year = null
            viewModel.month = null
            viewModel.approxTime = null
            return
        }

        if (date.contains("過去")) {
            viewModel.approxTime = date
            viewModel.year = null
            viewModel.month = null
        } else {
            viewModel.approxTime = null
            val regex = """(\d+)\s*年(?:\s*(\d+)\s*月)?""".toRegex()
            val match = regex.find(date)
            if (match != null) {
                val (y, m) = match.destructured
                viewModel.year = y.toIntOrNull()
                viewModel.month = m.toIntOrNull()
            } else {
                viewModel.year = null
                viewModel.month = null
            }
        }
    }
}
