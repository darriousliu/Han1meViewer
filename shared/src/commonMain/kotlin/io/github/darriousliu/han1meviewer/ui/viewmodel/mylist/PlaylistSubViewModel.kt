package io.github.darriousliu.han1meviewer.ui.viewmodel.mylist

import io.github.darriousliu.han1meviewer.core.common.EMPTY_STRING
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.repository.NetworkRepo
import io.github.darriousliu.han1meviewer.core.model.HanimeInfo
import io.github.darriousliu.han1meviewer.core.model.ModifiedPlaylistArgs
import io.github.darriousliu.han1meviewer.core.model.MyListItems
import io.github.darriousliu.han1meviewer.core.model.Playlists
import io.github.darriousliu.han1meviewer.core.common.state.PageLoadingState
import io.github.darriousliu.han1meviewer.core.common.state.WebsiteState
import io.github.darriousliu.han1meviewer.core.network.CsrfTokenStore.csrfToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlaylistSubViewModel(
    private val scope: CoroutineScope,
) {

    var playlistPage = 1
    var playlistCode: String? = null
    var playlistTitle: String? = null
    var playlistDesc: String? = null

    private val _playlistsFlow =
        MutableStateFlow<WebsiteState<Playlists>>(WebsiteState.Loading)
    val playlistsFlow = _playlistsFlow.asStateFlow()
    val userId = Preferences.savedUserId

    fun getPlaylists(page: Int = 1) {
        scope.launch {
            NetworkRepo.getPlaylists(page, userId).collect {
                _playlistsFlow.value = it
            }
        }
    }

    private val _playlistStateFlow =
        MutableStateFlow<PageLoadingState<MyListItems<HanimeInfo>>>(PageLoadingState.Loading)
    val playlistStateFlow = _playlistStateFlow.asStateFlow()

    private val _playlistFlow = MutableStateFlow(emptyList<HanimeInfo>())
    val playlistFlow = _playlistFlow.asStateFlow()

    fun getPlaylistItems(page: Int, listCode: String) {
        val userId = Preferences.savedUserId
        scope.launch {
            NetworkRepo.getMyListItems(userId, listCode, page).collect { state ->
                val prev = _playlistStateFlow.getAndUpdate { state }
                if (prev is PageLoadingState.Loading) _playlistFlow.value = emptyList()
                _playlistFlow.update { prevList ->
                    when (state) {
                        is PageLoadingState.Success -> (prevList + state.info.hanimeInfo)
                            .distinctBy(HanimeInfo::videoCode)
                        is PageLoadingState.Loading -> emptyList()
                        else -> prevList
                    }
                }
            }
        }
    }

    private val _deleteFromPlaylistFlow = MutableSharedFlow<WebsiteState<Int>>()
    val deleteFromPlaylistFlow = _deleteFromPlaylistFlow.asSharedFlow()

    fun deleteFromPlaylist(listCode: String, videoCode: String, position: Int) {
        scope.launch {
            NetworkRepo.deleteMyListItems(listCode, videoCode, position, csrfToken).collect {
                _deleteFromPlaylistFlow.emit(it)
                _playlistFlow.update { prevList ->
                    if (it is WebsiteState.Success) {
                        prevList.toMutableList().apply { removeAt(position) }
                    } else prevList
                }
            }
        }
    }

    private val _modifyPlaylistFlow = MutableSharedFlow<WebsiteState<ModifiedPlaylistArgs>>()
    val modifyPlaylistFlow = _modifyPlaylistFlow.asSharedFlow()

    fun modifyPlaylist(listCode: String, title: String, desc: String, delete: Boolean) {
        scope.launch {
            NetworkRepo.modifyPlaylist(listCode, title, desc, delete, csrfToken).collect {
                _modifyPlaylistFlow.emit(it)
                if (delete) {
                    clearMyListItems()
                }
            }
        }
    }

    private val _createPlaylistFlow = MutableSharedFlow<WebsiteState<Unit>>()
    val createPlaylistFlow = _createPlaylistFlow.asSharedFlow()

    fun createPlaylist(title: String, description: String) {
        scope.launch {
            NetworkRepo.createPlaylist(EMPTY_STRING, title, description, csrfToken).collect {
                _createPlaylistFlow.emit(it)
            }
        }
    }

    fun clearMyListItems() {
        _playlistStateFlow.value = PageLoadingState.Loading
    }
}
