package io.github.darriousliu.han1meviewer.feature.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.darriousliu.han1meviewer.core.repository.NetworkRepo
import io.github.darriousliu.han1meviewer.core.model.HanimePreview
import io.github.darriousliu.han1meviewer.core.common.state.WebsiteState
import io.github.darriousliu.han1meviewer.core.model.TagLocalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/23 023 16:47
 */
@KoinViewModel
class PreviewViewModel : ViewModel() {

    private val previewCache = linkedMapOf<String, WebsiteState<HanimePreview>>()

    private val _previewFlow =
        MutableStateFlow<WebsiteState<HanimePreview>>(WebsiteState.Loading)
    val previewFlow = _previewFlow.asStateFlow()

    fun getHanimePreview(date: String) {
        viewModelScope.launch {
            previewCache[date]?.let {
                _previewFlow.value = it
                return@launch
            }
            NetworkRepo.getHanimePreview(date).collect { preview ->
                val localizedPreview = preview.withLocalizedTags()
                _previewFlow.value = localizedPreview
                if (localizedPreview !is WebsiteState.Loading) {
                    previewCache[date] = localizedPreview
                }
            }
        }
    }

    fun preloadPreview(date: String) {
        if (previewCache.containsKey(date)) return
        viewModelScope.launch {
            val preview = runCatching {
                withContext(Dispatchers.IO) {
                    NetworkRepo.getHanimePreview(date)
                        .catch { emit(WebsiteState.Error(it)) }
                        .first { it !is WebsiteState.Loading }
                }
            }.getOrElse { WebsiteState.Error(it) }
            previewCache[date] = preview.withLocalizedTags()
        }
    }

    fun getCachedPreview(date: String): WebsiteState<HanimePreview>? = previewCache[date]

    private suspend fun WebsiteState<HanimePreview>.withLocalizedTags(): WebsiteState<HanimePreview> {
        return if (this is WebsiteState.Success) {
            WebsiteState.Success(info.withLocalizedTags())
        } else {
            this
        }
    }

    private suspend fun HanimePreview.withLocalizedTags(): HanimePreview {
        return copy(previewInfo = previewInfo.map { info ->
            info.copy(tags = TagLocalizer.localizeTags(info.tags))
        })
    }
}
