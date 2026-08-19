package io.github.darriousliu.han1meviewer.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.github.darriousliu.han1meviewer.core.repository.DatabaseRepo
import io.github.darriousliu.han1meviewer.core.storage.entity.WatchHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

private val logger = Logger.withTag("LocalWatchHistoryViewModel")

/** 本地观看历史。 */
@KoinViewModel
class LocalWatchHistoryViewModel : ViewModel() {

    fun deleteWatchHistory(history: WatchHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.WatchHistory.delete(history)
            logger.d { "$history DONE!" }
        }
    }

    fun deleteAllWatchHistories() {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.WatchHistory.deleteAll()
            logger.d { "delete all watch history DONE!" }
        }
    }

    fun loadAllWatchHistories() =
        DatabaseRepo.WatchHistory.loadAll()
            .catch { e -> e.printStackTrace() }
            .flowOn(Dispatchers.IO)
}
