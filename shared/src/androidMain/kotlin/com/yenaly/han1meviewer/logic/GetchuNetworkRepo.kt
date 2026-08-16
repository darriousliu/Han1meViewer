package com.yenaly.han1meviewer.logic

import android.util.Log
import com.yenaly.han1meviewer.logic.NetworkRepo.handleException
import com.yenaly.han1meviewer.logic.NetworkRepo.throwRequestException
import com.yenaly.han1meviewer.logic.network.HanimeNetwork
import com.yenaly.han1meviewer.logic.state.WebsiteState
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.nio.charset.Charset

object GetchuNetworkRepo {
    val GETCHU_CHARSET: Charset = Charset.forName("EUC-JP")
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
            Log.d(
                "GetchuPreviewParser",
                "series ajax id=$id parentId=$parentId items=${seriesItems.size}"
            )
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

    /**
     * getchu 的页面是 EUC-JP 编码，Ktor 的 `bodyAsText()` 按 UTF-8 解会全是乱码，
     * 所以拿原始字节自己解。
     *
     * ⚠️ 这一步是本次迁移里少数**没能进 commonMain** 的地方：Kotlin/Native 上 Ktor 的
     * charset 支持只有 UTF-8，EUC-JP 得走 `NSString.create(data:encoding:)`。
     * 等 iOS 真要接 getchu 时再补 expect/actual。
     */
    private suspend fun HttpResponse.getchuString(): String {
        return body<ByteArray>().toString(GETCHU_CHARSET)
    }

    private fun String.extractGetchuSeriesParentId(): String? {
        return Regex("[\"']parent_id_array[\"']\\s*:\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            .find(this)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }
    }
}
