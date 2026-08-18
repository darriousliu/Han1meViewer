package io.github.darriousliu.han1meviewer.core.network.service

import de.jensklingenberg.ktorfit.http.Field
import de.jensklingenberg.ktorfit.http.FormUrlEncoded
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query
import de.jensklingenberg.ktorfit.http.Url
import io.ktor.client.statement.HttpResponse

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/08 008 22:10
 */
interface HanimeBaseService {

    @GET
    suspend fun getHomePage(@Url url: String): HttpResponse

    @GET("search")
    suspend fun getHanimeSearchResult(
        @Query("page") page: Int = 1,
        @Query("query") query: String? = null,
        @Query("genre") genre: String? = null,
        @Query("sort") sort: String? = null,
        @Query("broad") broad: String? = null,
//        @Query("year") year: Int? = null,
//        @Query("month") month: Int? = null,
        @Query("date") date: String? = null,
        @Query("duration") duration: String? = null,
        @Query("tags[]") tags: List<String> = emptyList(),
        @Query("brands[]") brands: List<String> = emptyList(),
    ): HttpResponse

    @GET("watch")
    suspend fun getHanimeVideo(
        @Query("v") videoCode: String,
    ): HttpResponse

    @GET("previews/{date}")
    suspend fun getHanimePreview(
        @Path("date") date: String, // 类似 202206. 202012
    ): HttpResponse

    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("_token") csrfToken: String?,
        @Field("email") email: String,
        @Field("password") password: String,
        @Header("X-CSRF-TOKEN") csrfToken_1: String? = csrfToken,
    ): HttpResponse

    @GET("login")
    suspend fun getLoginPage(): HttpResponse

    @GET("subscriptions")
    suspend fun getMySubscriptions(
        @Query("page") page: Int,
    ): HttpResponse
}
