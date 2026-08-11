package com.yenaly.han1meviewer.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.yenaly.han1meviewer.ui.viewmodel.mylist.FavSubViewModel
import com.yenaly.han1meviewer.ui.viewmodel.mylist.PlaylistSubViewModel
import com.yenaly.han1meviewer.ui.viewmodel.mylist.WatchLaterSubViewModel

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/07/04 004 22:46
 */
class MyListViewModel : ViewModel() {

    val playlist by lazy(LazyThreadSafetyMode.NONE) { PlaylistSubViewModel() }
    val watchLater by lazy(LazyThreadSafetyMode.NONE) { WatchLaterSubViewModel() }
    val fav by lazy(LazyThreadSafetyMode.NONE) { FavSubViewModel() }
}
