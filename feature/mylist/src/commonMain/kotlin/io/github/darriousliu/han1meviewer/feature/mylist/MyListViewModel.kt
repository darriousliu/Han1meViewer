package io.github.darriousliu.han1meviewer.feature.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.koin.android.annotation.KoinViewModel

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/07/04 004 22:46
 */
@KoinViewModel
class MyListViewModel : ViewModel() {

    val playlist = PlaylistSubViewModel(viewModelScope)
    val watchLater = WatchLaterSubViewModel(viewModelScope)
    val fav = FavSubViewModel(viewModelScope)
}
