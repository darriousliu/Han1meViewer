package io.github.darriousliu.han1meviewer.logic

import co.touchlab.kermit.Logger
import io.github.darriousliu.han1meviewer.logic.NetworkRepo.handleException
import io.github.darriousliu.han1meviewer.logic.NetworkRepo.throwRequestException
import io.github.darriousliu.han1meviewer.logic.network.HanimeNetwork
import io.github.darriousliu.han1meviewer.core.common.state.WebsiteState
import io.github.darriousliu.han1meviewer.core.common.util.decodeEucJp
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

object GetchuNetworkRepo {
    fun getGetchuPreview(date: String) = websiteIOFlow(
        request = {
            HanimeNetwork.getchuService.getPreviewList(
                year = date.take(4),
                month = date.takeLast(2),
            )
        },
        bodyToString = { it.getchuString() },
        action = { GetchuParser.getchuPreview(it, date) }
    )
    fun getGetchuPreviewDetail(id: String) = websiteIOFlow(
        request = { HanimeNetwork.getchuService.getPreviewDetail(id) },
        bodyToString = { it.getchuString() },
    ) { body ->
        val detailState = GetchuParser.getchuPreviewDetail(body, id)
        if (detailState !is WebsiteState.Success) return@websiteIOFlow detailState

        val parentId = body.extractGetchuSeriesParentId() ?: return@websiteIOFlow detailState
        runCatching {
            val response = HanimeNetwork.getchuService.getSeriesItems(parentIdArray = parentId)
            if (!response.status.isSuccess()) return@runCatching emptyList()
            GetchuParser.getchuSeriesItems(response.getchuString())
        }.getOrDefault(emptyList()).let { seriesItems ->
            Logger.d(tag = "GetchuPreviewParser") {
                "series ajax id=$id parentId=$parentId items=${seriesItems.size}"
            }
            if (seriesItems.isEmpty()) {
                detailState
            } else {
                val detail = detailState.info
                val mergedSeriesItems = (detail.seriesItems + seriesItems)
                    .distinctBy { it.id }
                    .filterNot { it.id == id }
                WebsiteState.Success(
                    detail.copy(
                        seriesItems = mergedSeriesItems,
                        relatedItems = mergedSeriesItems,
                    )
                )
            }
        }
    }
    private fun <T> websiteIOFlow(
        request: suspend () -> HttpResponse,
        permittedSuccessCode: IntArray? = null,
        bodyToString: suspend (HttpResponse) -> String = { it.bodyAsText() },
        action: suspend (String) -> WebsiteState<T>,
    ) = flow {
        val requestResult = request.invoke()
        val resultBody = bodyToString(requestResult)
        val permitted = permittedSuccessCode?.contains(requestResult.status.value) == true
        if ((permitted || requestResult.status.isSuccess())) {
            emit(action.invoke(resultBody))
        } else {
            requestResult.throwRequestException()
        }
    }.catch { e ->
        emit(WebsiteState.Error(handleException(e)))
    }.flowOn(Dispatchers.IO)

    /** getchu 的页面是 EUC-JP，见 [decodeEucJp]。 */
    private suspend fun HttpResponse.getchuString(): String = body<ByteArray>().decodeEucJp()

    private fun String.extractGetchuSeriesParentId(): String? {
        return Regex("[\"']parent_id_array[\"']\\s*:\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }
    }
}
