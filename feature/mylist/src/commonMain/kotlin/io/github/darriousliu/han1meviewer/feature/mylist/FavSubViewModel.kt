package io.github.darriousliu.han1meviewer.feature.mylist

import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.repository.NetworkRepo
import io.github.darriousliu.han1meviewer.core.model.HanimeInfo
import io.github.darriousliu.han1meviewer.core.model.MyListItems
import io.github.darriousliu.han1meviewer.core.model.MyListType
import io.github.darriousliu.han1meviewer.core.common.state.PageLoadingState
import io.github.darriousliu.han1meviewer.core.common.state.WebsiteState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class FavSubViewModel(scope: CoroutineScope) : MyListSubViewModel(scope) {

    var favVideoPage = 1
    private var csrfToken: String? = null

    val favVideoStateFlow: StateFlow<PageLoadingState<MyListItems<HanimeInfo>>> = itemsStateFlow.asStateFlow()
    val favVideoFlow: StateFlow<List<HanimeInfo>> = itemsFlow.asStateFlow()

    fun getMyFavVideoItems(userId: String, page: Int) {
        loadItems(MyListType.FAV_VIDEO, userId, page) { csrfToken = it.csrfToken }
    }

    private val _deleteMyFavVideoFlow = MutableSharedFlow<WebsiteState<Boolean>>()
    val deleteMyFavVideoFlow = _deleteMyFavVideoFlow.asSharedFlow()

    fun deleteMyFavVideo(videoCode: String, position: Int) {
        deleteItem(
            deleteCall = {
                NetworkRepo.addToMyFavVideo(
                    videoCode = videoCode,
                    likeStatus = true,
                    currentUserId = Preferences.savedUserId,
                    token = csrfToken,
                )
            },
            emitTo = _deleteMyFavVideoFlow,
            position = position,
            mapState = { it },
        )
    }

    override fun clearMyListItems() {
        super.clearMyListItems()
        favVideoPage = 1
    }
}
