package io.github.darriousliu.han1meviewer.ui.viewmodel.mylist

import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.repository.NetworkRepo
import io.github.darriousliu.han1meviewer.core.model.HanimeInfo
import io.github.darriousliu.han1meviewer.core.model.MyListItems
import io.github.darriousliu.han1meviewer.core.model.MyListType
import io.github.darriousliu.han1meviewer.core.common.state.PageLoadingState
import io.github.darriousliu.han1meviewer.core.common.state.WebsiteState
import io.github.darriousliu.han1meviewer.core.network.CsrfTokenStore.csrfToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class WatchLaterSubViewModel(scope: CoroutineScope) : MyListSubViewModel(scope) {

    var watchLaterPage = 1

    val watchLaterStateFlow: StateFlow<PageLoadingState<MyListItems<HanimeInfo>>> = itemsStateFlow.asStateFlow()
    val watchLaterFlow: StateFlow<List<HanimeInfo>> = itemsFlow.asStateFlow()

    fun getMyWatchLaterItems(page: Int) {
        loadItems(MyListType.WATCH_LATER, Preferences.savedUserId, page)
    }

    private val _deleteMyWatchLaterFlow = MutableSharedFlow<WebsiteState<Boolean>>()
    val deleteMyWatchLaterFlow = _deleteMyWatchLaterFlow.asSharedFlow()

    fun deleteMyWatchLater(videoCode: String, position: Int) {
        deleteItem(
            deleteCall = {
                NetworkRepo.addToMyList(
                    listCode = "save",
                    videoCode = videoCode,
                    isChecked = false,
                    position = position,
                    csrfToken = csrfToken,
                )
            },
            emitTo = _deleteMyWatchLaterFlow,
            position = position,
            mapState = { state ->
                when (state) {
                    is WebsiteState.Error -> WebsiteState.Error(state.throwable)
                    WebsiteState.Loading -> WebsiteState.Loading
                    is WebsiteState.Success -> WebsiteState.Success(true)
                }
            },
        )
    }

    override fun clearMyListItems() {
        super.clearMyListItems()
        watchLaterPage = 1
    }
}
