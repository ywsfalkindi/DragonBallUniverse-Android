package com.saiyan.dragonballuniverse.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * PocketBase public Records API.
 *
 * We use:
 * GET /api/collections/{collection}/records
 *
 * Common query params:
 * - filter: e.g. is_published=true && arc="super"
 * - sort: e.g. chapter_number
 * - perPage/page: pagination
 */
interface PocketBaseService {
    @GET("collections/manga_chapters/records")
    suspend fun listMangaChapters(
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int? = null,
        @Query("perPage") perPage: Int? = null,
    ): PocketBaseListResponse<PocketBaseMangaChapterRecord>

    @GET("collections/quiz_questions/records")
    suspend fun listQuizQuestions(
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int? = null,
        @Query("perPage") perPage: Int? = null,
    ): PocketBaseListResponse<PocketBaseQuizQuestionRecord>

    @GET("collections/user_stats/records")
    suspend fun listUserStats(
        @Query("filter") filter: String? = null,
        @Query("page") page: Int? = null,
        @Query("perPage") perPage: Int? = null,
    ): PocketBaseListResponse<PocketBaseUserStatsRecord>

    @POST("collections/user_stats/records")
    suspend fun createUserStats(
        @Body body: PocketBaseUpsertUserStatsBody,
    ): PocketBaseUserStatsRecord

    @PATCH("collections/user_stats/records/{id}")
    suspend fun updateUserStats(
        @Path("id") id: String,
        @Body body: PocketBaseUpsertUserStatsBody,
    ): PocketBaseUserStatsRecord
}
