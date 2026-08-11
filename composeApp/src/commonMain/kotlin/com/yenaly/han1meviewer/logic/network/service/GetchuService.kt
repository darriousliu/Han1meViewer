package com.yenaly.han1meviewer.logic.network.service

import com.yenaly.han1meviewer.logic.network.RawNetworkResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters

class GetchuService(
    client: HttpClient,
    baseUrl: String,
) {
    private val transport = KtorServiceTransport(client, baseUrl)

    suspend fun getPreviewList(
        genre: String = "anime_dvd",
        gage: String = "adult",
        year: String,
        month: String,
        gc: String = "gc",
    ): RawNetworkResponse = transport.raw(pathSegments = listOf("all", "month_title.html")) {
        url.parameters.append("genre", genre)
        url.parameters.append("gage", gage)
        url.parameters.append("year", year)
        url.parameters.append("month", month)
        url.parameters.append("gc", gc)
    }

    suspend fun getPreviewDetail(
        id: String,
        gc: String = "gc",
    ): RawNetworkResponse = transport.raw(pathSegments = listOf("item", id, "")) {
        url.parameters.append("gc", gc)
    }

    suspend fun getSeriesItems(
        productIdArray: String = "",
        parentIdArray: String,
        genre: String = "anime_dvd",
        subGenreArray: String = "",
        naSubGenreArray: String = "",
        subGenrePerfectMatching: String = "",
        brandIdArray: String = "",
        age: String = "",
        stockFlag: String = "",
        sortCondition: String = "release_date",
        sortOrder: String = "asc",
        limitCount: String = "30",
        limitCountLower: String = "1",
        imageExist: String = "",
        startDate: String = "",
        endDate: String = "",
        noveltyFlag: String = "",
        templateHtml: String = "item-series/item-series.html",
        paging: String = "",
        pageSize: String = "",
        javascriptId: String = "",
        searchWord: String = "",
        limitless: String = "1",
        lowerLimit: String = "",
        upperLimit: String = "",
        imageSize: String = "s",
        addQuery: String = "",
    ): RawNetworkResponse = transport.raw(
        method = HttpMethod.Post,
        pathSegments = listOf("util", "GetchuSearch", "GetchuSearchAjax.php"),
    ) {
        setBody(FormDataContent(Parameters.build {
            append("product_id_array", productIdArray)
            append("parent_id_array", parentIdArray)
            append("genre", genre)
            append("sub_genre_array", subGenreArray)
            append("NA_sub_genre_array", naSubGenreArray)
            append("sub_genre_perfect_matching", subGenrePerfectMatching)
            append("brand_id_array", brandIdArray)
            append("age", age)
            append("stock_flag", stockFlag)
            append("sort_condition", sortCondition)
            append("sort_order", sortOrder)
            append("limit_count", limitCount)
            append("limit_count_lower", limitCountLower)
            append("image_exist", imageExist)
            append("start_date", startDate)
            append("end_date", endDate)
            append("novelty_flag", noveltyFlag)
            append("template_html", templateHtml)
            append("paging", paging)
            append("page_size", pageSize)
            append("javascript_id", javascriptId)
            append("search_word", searchWord)
            append("limitless", limitless)
            append("lower_limit", lowerLimit)
            append("upper_limit", upperLimit)
            append("image_size", imageSize)
            append("add_query", addQuery)
        }))
    }
}
